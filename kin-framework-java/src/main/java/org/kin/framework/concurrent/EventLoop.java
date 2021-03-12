package org.kin.framework.concurrent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 除了executor能力外, 还具备处理{@link Message}的能力
 * 由{@link EventExecutorGroup}来进行实例分配
 * 目前有两种实现:
 * 1. {@link OrderedEventLoop}
 * 2. {@link SingleThreadEventLoop}
 * <p>
 * 两者的区别在于{@link SingleThreadEventLoop}所有消息处理都在同一线程,
 * 而{@link OrderedEventLoop}保证消息按顺序处理, 但不保证在同一线程处理, 故{@link ThreadLocal}和{@link FastThreadLocal}都不能使用
 * <p>
 * 性能:
 * 1. {@link SingleThreadEventLoop}要比{@link OrderedEventLoop}好, 接近netty DefaultEventExecutor
 * 2. {@link OrderedEventLoop}性能接近于基于{@link java.util.concurrent.DelayQueue}实现的SingleThreadEventLoop(之前的版本, 看git)
 * 当前版本的{@link SingleThreadEventLoop}调度触发只能是在event loop内, 而之前版本基于{@link java.util.concurrent.DelayQueue}实现的SingleThreadEventLoop,
 * 则支持非event loop内触发(异步)
 *
 * @author huangjianqin
 * @date 2021/3/10
 */
public interface EventLoop<EL extends EventLoop<EL>> extends EventExecutor, EventLoopGroup<EL> {
    @Override
    @SuppressWarnings("unchecked")
    default EL next() {
        return (EL) this;
    }

    @Override
    EventLoopGroup<EL> parent();

    /**
     * {@link Message}转换成{@link Runnable}
     */
    @SuppressWarnings("unchecked")
    default Runnable message2Runnable(Message<EL> message) {
        return () -> message.handle((EL) this);
    }

    /**
     * {@link Runnable}转换成{@link Message}
     */
    @SuppressWarnings("unchecked")
    default Message<EL> runnable2Message(Runnable runnable) {
        return p -> runnable.run();
    }

    @Override
    default void receive(Message<EL> message) {
        execute(message2Runnable(message));
    }

    @Override
    default ScheduledFuture<?> schedule(Message<EL> message, long delay, TimeUnit unit) {
        return schedule(message2Runnable(message), delay, unit);
    }

    @Override
    default ScheduledFuture<?> scheduleAtFixedRate(Message<EL> message, long initialDelay, long period, TimeUnit unit) {
        return scheduleAtFixedRate(message2Runnable(message), initialDelay, period, unit);
    }

    @Override
    default ScheduledFuture<?> scheduleWithFixedDelay(Message<EL> message, long initialDelay, long period, TimeUnit unit) {
        return scheduleWithFixedDelay(message2Runnable(message), initialDelay, period, unit);
    }
}
