package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledDispatcher extends Dispatcher {
    /**
     * 延迟调度事件分发
     *
     * @param event  事件
     * @param unit   时间单位
     * @param time   延迟执行事件
     * @param params 额外参数
     */
    Future<?> scheduleDispatch(Object event, TimeUnit unit, long time, Object... params);

    /**
     * 定时调度事件分发
     *
     * @param event        事件
     * @param unit         时间单位
     * @param initialDelay 延迟执行事件
     * @param period       事件间隔
     * @param params       额外参数
     */
    Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period, Object... params);
}
