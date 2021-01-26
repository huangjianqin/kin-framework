package org.kin.framework.concurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 固定数量的{@link PinnedThreadExecutor}池
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
public final class FixPinnedThreadExecutorPool<T extends PinnedThreadExecutor<T>> {
    /** 线程池 */
    private final ExecutionContext executionContext;
    /** PinnedThreadExecutor缓存 */
    private final List<T> executors;

    public FixPinnedThreadExecutorPool(int parallelism, ExecutionContext ec) {
        this(parallelism, ec, PinnedThreadExecutorBuilder.DEFAULT);
    }

    @SuppressWarnings("unchecked")
    public FixPinnedThreadExecutorPool(int parallelism, ExecutionContext ec, PinnedThreadExecutorBuilder<T> builder) {
        this.executionContext = ec;
        List<T> executors = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; i++) {
            executors.add(builder.build(this.executionContext));
        }
        this.executors = Collections.unmodifiableList(executors);
    }

    /**
     * 根据索引获取PinnedThreadExecutor实例
     */
    public T get(int index) {
        return executors.get(index);
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
