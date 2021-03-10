package org.kin.framework.concurrent;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * EventLoop组
 *
 * @author huangjianqin
 * @date 2021/3/10
 */
public interface EventLoopGroup<EL extends EventLoop<EL>> extends EventExecutorGroup {
    @Override
    EL next();

    /**
     * 接收消息
     */
    default void receive(Message<EL> message) {
        next().receive(message);
    }

    /**
     * 调度处理消息
     */
    default ScheduledFuture<?> schedule(Message<EL> message, long delay, TimeUnit unit) {
        if (!isShutdown()) {
            return next().schedule(message, delay, unit);
        }
        throw new IllegalStateException("executor is stopped");
    }

    /**
     * 固定速率处理消息
     */
    default ScheduledFuture<?> scheduleAtFixedRate(Message<EL> message, long initialDelay, long period, TimeUnit unit) {
        if (!isShutdown()) {
            return next().scheduleAtFixedRate(message, initialDelay, period, unit);
        }
        throw new IllegalStateException("executor is stopped");
    }

    /**
     * 固定延迟处理消息
     */
    default ScheduledFuture<?> scheduleWithFixedDelay(Message<EL> message, long initialDelay, long period, TimeUnit unit) {
        if (!isShutdown()) {
            return next().scheduleWithFixedDelay(message, initialDelay, period, unit);
        }
        throw new IllegalStateException("executor is stopped");
    }
}
