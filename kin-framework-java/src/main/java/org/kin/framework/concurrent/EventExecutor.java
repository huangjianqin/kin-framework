package org.kin.framework.concurrent;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
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

    /**
     * Return a new {@link Promise}.
     *
     * @see DefaultPromise
     */
    default <V> Promise<V> newPromise() {
        return new DefaultPromise<>(this);
    }

    /**
     * Create a new {@link ProgressivePromise}.
     *
     * @see DefaultProgressivePromise
     */
    default <V> ProgressivePromise<V> newProgressivePromise() {
        return new DefaultProgressivePromise<>(this);
    }
}
