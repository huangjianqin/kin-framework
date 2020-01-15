package org.kin.framework.concurrent.lock;

import org.kin.framework.concurrent.lock.exception.LockRunFailException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 锁任务
 * 防止死锁而导致业务逻辑卡死
 *
 * @author huangjianqin
 * @date 2020-01-15
 */
public class LockBox<K extends Comparable<K>> {
    //用于没有外部自定义锁实现的key存储锁对象
    private ConcurrentHashMap<K, Lock> lockMap = new ConcurrentHashMap<>();
    //当前锁线程等待的锁列表
    private ThreadLocal<List<LockInfo>> threadLocal = new ThreadLocal<>();

    public void execute(K key, Runnable runnable) {
        if (!lockMap.containsKey(key)) {
            synchronized (lockMap) {
                if (!lockMap.containsKey(key)) {
                    lockMap.put(key, new ReentrantReadWriteLock().readLock());
                }
            }
        }
        Lock newLock = lockMap.get(key);
        execute(key, newLock, runnable);
    }

    public void execute(K key, Lock lock, Runnable runnable) {
        List<LockInfo> curThreadLocks = threadLocal.get();
        if (curThreadLocks == null) {
            curThreadLocks = new ArrayList<>();
            threadLocal.set(curThreadLocks);
        }
        LockInfo lockInfo = new LockInfo(key, lock, runnable);
        curThreadLocks.add(lockInfo);
        Collections.sort(curThreadLocks);

        if (!lockInfo.getLock().tryLock()) {
            //加锁失败
            //全部锁释放
            for (LockInfo curThreadLockInfo : curThreadLocks) {
                curThreadLockInfo.getLock().unlock();
            }

            //尝试重新获取锁
            for (LockInfo curThreadLockInfo : curThreadLocks) {
                try {
                    //超时失败, 是为了防止业务尝试获取锁等待太久了, 理论上业务不应该存在这样子的逻辑, 理应优化
                    if (!curThreadLockInfo.getLock().tryLock(200, TimeUnit.MILLISECONDS)) {
                        //尝试获取锁失败
                        for (LockInfo curThreadLockInfo1 : curThreadLocks) {
                            curThreadLockInfo1.getLock().unlock();
                        }
                        //抛异常
                        throw new LockRunFailException("try get lock and run fail");
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        //加锁成功
        try {
            lockInfo.getRunnable().run();
        } finally {
            lockInfo.getLock().unlock();
            curThreadLocks.remove(lockInfo);
        }
    }

    private class LockInfo implements Comparable<LockInfo> {
        private K key;
        private Lock lock;
        private Runnable runnable;

        public LockInfo(K key, Lock lock, Runnable runnable) {
            this.key = key;
            this.lock = lock;
            this.runnable = runnable;
        }

        //getter
        public K getKey() {
            return key;
        }

        public Lock getLock() {
            return lock;
        }

        public Runnable getRunnable() {
            return runnable;
        }

        @Override
        public int compareTo(LockInfo o) {
            return key.compareTo(o.getKey());
        }
    }
}
