package org.kin.framework.concurrent;

import java.util.LinkedList;
import java.util.List;

/**
 * 可无限创建{@link PinnedThreadExecutor}实例的对象池
 * 但底层线程池是固定的
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
public class CachedPinnedThreadExecutorPool<T extends PinnedThreadExecutor<T>> {
    /** 线程池 */
    private final ExecutionContext executionContext;
    /** 自定义{@link PinnedThreadExecutor}实例构建逻辑 */
    private final PinnedThreadExecutorBuilder<T> builder;
    /** PinnedThreadExecutor缓存 */
    private final List<T> executors = new LinkedList<>();

    public CachedPinnedThreadExecutorPool(ExecutionContext ec) {
        this(ec, PinnedThreadExecutorBuilder.DEFAULT);
    }

    public CachedPinnedThreadExecutorPool(ExecutionContext ec, PinnedThreadExecutorBuilder<T> builder) {
        this.executionContext = ec;
        this.builder = builder;
    }

    /**
     * 根据索引获取PinnedThreadExecutor实例
     */
    public T get() {
        T executor = builder.build(executionContext);
        executors.add(executor);
        return executor;
    }

    /**
     * 归还PinnedThreadExecutor实例
     * 调用此方法后, 外部不应该继续使用该PinnedThreadExecutor实例
     */
    public void returnExecutor(T executor) {
        if (executors.remove(executor)) {
            //存在, 则执行stop
            executor.stop();
        }
    }

    /**
     * shutdown
     */
    public void shutdown() {
        for (T executor : executors) {
            executor.stop();
        }

        executionContext.shutdown();
    }
}
