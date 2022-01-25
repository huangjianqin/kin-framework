package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;

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
    /** 默认scheduler线程数 */
    private static final int DEFAULT_SCHEDULER_PARALLELISM = 3;

    /** 线程池 */
    private final ExecutionContext executionContext;
    /** 自定义{@link OrderedEventLoop}实例构建逻辑 */
    private final OrderedEventLoopBuilder<P> builder;
    /** {@link OrderedEventLoop}缓存 */
    private final List<P> executors = new LinkedList<>();

    public static <P extends OrderedEventLoop<P>> CachedOrderedEventLoopGroup<P> fix(int coreSize, OrderedEventLoopBuilder<P> builder) {
        return fix(coreSize, "cachedOrderedEventLoopGroup", builder);
    }

    public static <P extends OrderedEventLoop<P>> CachedOrderedEventLoopGroup<P> fix(int coreSize, String workerNamePrefix, OrderedEventLoopBuilder<P> builder) {
        return new CachedOrderedEventLoopGroup<>(ExecutionContext.fix(coreSize, workerNamePrefix, DEFAULT_SCHEDULER_PARALLELISM), builder);
    }

    public static <P extends OrderedEventLoop<P>> CachedOrderedEventLoopGroup<P> cache(OrderedEventLoopBuilder<P> builder) {
        return cache("cachedOrderedEventLoopGroup", builder);
    }

    public static <P extends OrderedEventLoop<P>> CachedOrderedEventLoopGroup<P> cache(String workerNamePrefix, OrderedEventLoopBuilder<P> builder) {
        return new CachedOrderedEventLoopGroup<>(ExecutionContext.cache(workerNamePrefix, DEFAULT_SCHEDULER_PARALLELISM), builder);
    }

    public static <P extends OrderedEventLoop<P>> CachedOrderedEventLoopGroup<P> elastic(int coreSize, int maxSize, OrderedEventLoopBuilder<P> builder) {
        return elastic(coreSize, maxSize, "cachedOrderedEventLoopGroup", builder);
    }

    public static <P extends OrderedEventLoop<P>> CachedOrderedEventLoopGroup<P> elastic(int coreSize, int maxSize, String workerNamePrefix, OrderedEventLoopBuilder<P> builder) {
        return new CachedOrderedEventLoopGroup<>(ExecutionContext.elastic(coreSize, maxSize, workerNamePrefix, DEFAULT_SCHEDULER_PARALLELISM), builder);
    }

    public CachedOrderedEventLoopGroup(ExecutionContext ec, OrderedEventLoopBuilder<P> builder) {
        Preconditions.checkArgument(ec.withSchedule(), "execution context must be with scheduler");
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
