package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledEventBus extends EventBus {
    /**
     * 延迟调度事件分发
     *
     * @param event 事件
     * @param time  延迟执行事件
     * @param unit  时间单位
     */
    Future<?> schedule(Object event, long time, TimeUnit unit);

    /**
     * 固定时间间隔调度事件分发
     *
     * @param event        事件
     * @param initialDelay 延迟执行时间
     * @param period       时间间隔
     * @param unit         时间单位
     */
    Future<?> scheduleAtFixRate(Object event, long initialDelay, long period, TimeUnit unit);

    /**
     * 固定时间延迟调度事件分发
     *
     * @param event        事件
     * @param initialDelay 延迟执行时间
     * @param delay        时间延迟
     * @param unit         时间单位
     */
    Future<?> scheduleWithFixedDelay(Object event, long initialDelay, long delay, TimeUnit unit);
}
