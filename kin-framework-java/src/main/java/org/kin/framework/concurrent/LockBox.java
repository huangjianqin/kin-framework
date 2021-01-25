package org.kin.framework.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁盒
 * 执行不同id的锁任务, 并解决死锁问题
 *
 * @author huangjianqin
 * @date 2020-01-15
 */
public class LockBox<K extends Comparable<K>> {
    /** 用于没有外部自定义锁实现的key存储锁对象 */
    private ConcurrentHashMap<K, Lock> lockMap = new ConcurrentHashMap<>();
    /** 当前锁线程等待的锁列表 */
    private ThreadLocal<List<LockInfo>> threadLocal = new ThreadLocal<>();

    public void lockRun(K key, Runnable runnable) {
        if (!lockMap.containsKey(key)) {
            synchronized (lockMap) {
                if (!lockMap.containsKey(key)) {
                    lockMap.put(key, new ReentrantLock());
                }
            }
        }
        Lock newLock = lockMap.get(key);
        lockRun(key, newLock, runnable);
    }

    public void lockRun(K key, Lock lock, Runnable runnable) {
        List<LockInfo> curThreadLocks = threadLocal.get();
        if (curThreadLocks == null) {
            curThreadLocks = new ArrayList<>();
            threadLocal.set(curThreadLocks);
        }

        LockInfo lockInfo = new LockInfo(key, lock, runnable);
        curThreadLocks.add(lockInfo);

        try {
            if (!lockInfo.tryLock()) {
                //加锁失败
                //全部锁释放
                for (LockInfo curThreadLockInfo : curThreadLocks) {
                    curThreadLockInfo.unlock();
                }

                Collections.sort(curThreadLocks);

                //尝试重新获取锁
                for (LockInfo curThreadLockInfo : curThreadLocks) {
                    //超时失败, 是为了防止业务尝试获取锁等待太久了, 理论上业务不应该存在这么慢加锁业务逻辑(200ms足够), 理应优化
                    if (!curThreadLockInfo.tryLock(200, TimeUnit.MILLISECONDS)) {
                        //尝试获取锁失败
                        for (LockInfo curThreadLockInfo1 : curThreadLocks) {
                            curThreadLockInfo1.unlock();
                        }
                        //抛异常
                        throw new LockRunFailException("try get lock and run fail");
                    }
                }
            }

            //加锁成功
            try {
                lockInfo.getRunnable().run();
            } finally {
                lockInfo.unlock();
            }
        } finally {
            curThreadLocks.remove(lockInfo);
        }
    }

    private class LockInfo implements Comparable<LockInfo> {
        private K key;
        private Lock lock;
        private Runnable runnable;

        private boolean locking;

        public LockInfo(K key, Lock lock, Runnable runnable) {
            this.key = key;
            this.lock = lock;
            this.runnable = runnable;
        }

        public boolean tryLock() {
            if (locking) {
                return true;
            }
            locking = lock.tryLock();
            return locking;
        }

        public boolean tryLock(long time, TimeUnit unit) {
            if (locking) {
                return true;
            }
            try {
                locking = lock.tryLock(time, unit);
            } catch (InterruptedException e) {
            }
            return locking;
        }

        public void unlock() {
            if (!locking) {
                return;
            }
            lock.unlock();
            locking = false;
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

        @Override
        public String toString() {
            return Thread.currentThread().getName() + ">>>>>" + "LockInfo{" +
                    "key=" + key +
                    ", lock=" + lock +
                    ", locking=" + locking +
                    '}';
        }
    }
}
