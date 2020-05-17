package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2020-05-17
 */
public class PinnedThreadSafeFuturesManager {
    public static PinnedThreadSafeFuturesManager INSTANCE;

    /** key -> ActorLike实例 value -> 对应ActorLike已启动的调度 */
    private Map<PinnedThreadSafeHandler<?>, Queue<Future>> futures = new ConcurrentHashMap<>();
    private ExecutionContext executionContext = new ExecutionContext(ForkJoinPool.commonPool());

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
        //每1h清理已结束的调度
        executionContext.scheduleAtFixedRate(() -> futures.keySet().forEach(this::clearFinishedFutures), 0, 1, TimeUnit.HOURS);
    }

    public void register(PinnedThreadSafeHandler<?> threadSafeHandler) {
        futures.putIfAbsent(threadSafeHandler, new ConcurrentLinkedQueue<>());
    }

    public void unRegister(PinnedThreadSafeHandler<?> threadSafeHandler) {
        clearFutures(threadSafeHandler);
    }

    public void addFuture(PinnedThreadSafeHandler<?> threadSafeHandler, Future<?> future) {
        Queue<Future> queue;
        while ((queue = futures.putIfAbsent(threadSafeHandler, new ConcurrentLinkedQueue<>())) == null) {
        }
        queue.add(future);
    }

    public void clearFutures(PinnedThreadSafeHandler<?> threadSafeHandler) {
        Queue<Future> old = futures.remove(threadSafeHandler);
        if (old != null) {
            for (Future future : old) {
                if (!future.isDone() || !future.isCancelled()) {
                    future.cancel(true);
                }
            }
        }
    }

    private void clearFinishedFutures(PinnedThreadSafeHandler<?> threadSafeHandler) {
        Queue<Future> old = futures.get(threadSafeHandler);
        if (old != null) {
            old.removeIf(Future::isDone);
        }
    }

    public Map<PinnedThreadSafeHandler<?>, Queue<Future>> getFutures() {
        return futures;
    }
}
