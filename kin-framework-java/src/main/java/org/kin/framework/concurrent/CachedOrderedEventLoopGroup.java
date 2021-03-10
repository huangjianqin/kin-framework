package org.kin.framework.concurrent;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 可无限创建{@link OrderedEventLoop}实例的对象池
 * 但底层线程池是固定的
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
public class CachedOrderedEventLoopGroup<P extends OrderedEventLoop<P>> implements EventLoopGroup<P> {
    /** 线程池 */
    private final ExecutionContext executionContext;
    /** 自定义{@link OrderedEventLoop}实例构建逻辑 */
    private final OrderedEventLoopBuilder<P> builder;
    /** PinnedThreadExecutor缓存 */
    private final List<P> executors = new LinkedList<>();

    public CachedOrderedEventLoopGroup(ExecutionContext ec, OrderedEventLoopBuilder<P> builder) {
        this.executionContext = ec;
        this.builder = builder;
    }

    /**
     * shutdown
     */
    @Override
    public void shutdown() {
        for (P executor : executors) {
            executor.shutdown();
        }

        executionContext.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return executionContext.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executionContext.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return executionContext.awaitTermination(timeout, unit);
    }

    @Override
    public P next() {
        P executor = builder.build(this, executionContext);
        executors.add(executor);
        return executor;
    }
}
