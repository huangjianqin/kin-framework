package org.kin.framework.concurrent;

/**
 * 自定义选择{@link EventExecutor}实例逻辑
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
@FunctionalInterface
public interface EventExecutorChooser {
    /**
     * 选择{@link EventExecutor}实例逻辑
     */
    EventExecutor choose(EventExecutor[] executors);
}
