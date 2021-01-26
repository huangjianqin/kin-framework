package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2021/1/26
 */
@FunctionalInterface
public interface PinnedThreadExecutorBuilder<T extends PinnedThreadExecutor<T>> {
    PinnedThreadExecutorBuilder DEFAULT = PinnedThreadExecutor::new;

    /** 构建自定义{@link PinnedThreadExecutor} */
    T build(ExecutionContext ec);
}
