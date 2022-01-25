package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 固定数量的{@link OrderedEventLoop}池
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
public final class FixOrderedEventLoopGroup<P extends OrderedEventLoop<P>> implements EventLoopGroup<P> {
    /** 默认scheduler线程数 */
    private static final int DEFAULT_SCHEDULER_PARALLELISM = 3;
    /** 线程池 */
    private final ExecutionContext executionContext;
    /** {@link OrderedEventLoop}缓存 */
    private final List<P> executors;

    public FixOrderedEventLoopGroup(int executorSize, String workerNamePrefix, OrderedEventLoopBuilder<P> builder) {
        this(executorSize, ExecutionContext.fix(executorSize, workerNamePrefix, DEFAULT_SCHEDULER_PARALLELISM), builder);
    }

    public FixOrderedEventLoopGroup(int executorSize, ExecutionContext ec, OrderedEventLoopBuilder<P> builder) {
        Preconditions.checkArgument(ec.withSchedule(), "execution context must be with scheduler");
        this.executionContext = ec;
        List<P> executors = new ArrayList<>(executorSize);
        for (int i = 0; i < executorSize; i++) {
            executors.add(builder.build(this, this.executionContext));
        }
        this.executors = Collections.unmodifiableList(executors);
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
        //随机返回一个
        return next(ThreadLocalRandom.current().nextInt(executors.size()));
    }

    /**
     * 根据索引获取PinnedThreadExecutor实例
     */
    public P next(int index) {
        return executors.get(index % executors.size());
    }
}
