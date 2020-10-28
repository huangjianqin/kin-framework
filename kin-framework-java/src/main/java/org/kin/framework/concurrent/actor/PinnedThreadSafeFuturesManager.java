package org.kin.framework.concurrent.actor;

import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ExecutionContext;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-05-17
 * 管理{@link PinnedThreadSafeHandler} 创建的调度Future
 * 单例
 */
public class PinnedThreadSafeFuturesManager implements Closeable {
    public static PinnedThreadSafeFuturesManager INSTANCE;

    /** key -> PinnedThreadSafeHandler实例 value -> 对应PinnedThreadSafeHandler已启动的调度 */
    private Map<PinnedThreadSafeHandler<?>, Queue<Future>> futures = new ConcurrentHashMap<>();
    private ExecutionContext executionContext = ExecutionContext.fix(2, "pinnedThreadSafeFuturesWorker",
            2, "pinnedThreadSafeFuturesScheduler");

    public static PinnedThreadSafeFuturesManager instance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (PinnedThreadSafeFuturesManager.class) {
                if (Objects.isNull(INSTANCE)) {
                    INSTANCE = new PinnedThreadSafeFuturesManager();
                }
            }
        }

        return INSTANCE;
    }

    //------------------------------------------------------------------------------------------------------------------
    private PinnedThreadSafeFuturesManager() {
        monitorJVMClose();
        //每1h清理已结束的调度
        executionContext.scheduleAtFixedRate(() -> futures.keySet().forEach(this::clearUnvalidFutures), 0, 20, TimeUnit.MINUTES);
    }

    /**
     * 注册{@link PinnedThreadSafeHandler}
     */
    public void register(PinnedThreadSafeHandler<?> threadSafeHandler) {
        futures.putIfAbsent(threadSafeHandler, new ConcurrentLinkedQueue<>());
    }

    /**
     * 注销{@link PinnedThreadSafeHandler}
     */
    public void unRegister(PinnedThreadSafeHandler<?> threadSafeHandler) {
        /**
         * 取消所有未完成且未取消的Futures
         */
        Queue<Future> old = futures.remove(threadSafeHandler);
        if (old != null) {
            for (Future future : old) {
                if (!future.isDone() || !future.isCancelled()) {
                    future.cancel(true);
                }
            }
        }
    }

    /**
     * 关联PinnedThreadSafeHandler实例新构建的新Future
     */
    public void addFuture(PinnedThreadSafeHandler<?> threadSafeHandler, Future<?> future) {
        Queue<Future> queue;
        while ((queue = futures.putIfAbsent(threadSafeHandler, new ConcurrentLinkedQueue<>())) == null) {
        }
        queue.add(future);
    }

    /**
     * 移除无效(已完成或者已取消)的Futures
     */
    private void clearUnvalidFutures(PinnedThreadSafeHandler<?> threadSafeHandler) {
        Queue<Future> old = futures.get(threadSafeHandler);
        if (old != null) {
            old.removeIf(Future::isDone);
        }
    }

    public Map<PinnedThreadSafeHandler<?>, Queue<Future>> getFutures() {
        return futures;
    }

    @Override
    public void close() {
        executionContext.shutdown();
    }
}
