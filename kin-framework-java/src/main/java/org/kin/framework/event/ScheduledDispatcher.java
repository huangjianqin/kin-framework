package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledDispatcher extends EventDispatcher {
    /**
     * 延迟调度事件分发
     *
     * @param event 事件
     * @param unit  时间单位
     * @param time  延迟执行事件
     */
    Future<?> scheduleDispatch(Object event, TimeUnit unit, long time);

    /**
     * 固定时间间隔调度事件分发
     *
     * @param event        事件
     * @param unit         时间单位
     * @param initialDelay 延迟执行时间
     * @param period       时间间隔
     */
    Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period);

    /**
     * 固定时间延迟调度事件分发
     *
     * @param event        事件
     * @param unit         时间单位
     * @param initialDelay 延迟执行时间
     * @param delay        时间延迟
     */
    Future<?> scheduleDispatchWithFixedDelay(Object event, TimeUnit unit, long initialDelay, long delay);
}
