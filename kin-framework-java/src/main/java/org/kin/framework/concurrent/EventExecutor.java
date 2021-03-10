package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2021/1/25
 */
public interface EventExecutor extends EventExecutorGroup {
    @Override
    default EventExecutor next() {
        return this;
    }

    /** 所属group */
    EventExecutorGroup parent();

    /** 是否在同一线程loop */
    default boolean isInEventLoop() {
        return isInEventLoop(Thread.currentThread());
    }

    /** 是否在同一线程loop */
    boolean isInEventLoop(Thread thread);
}
