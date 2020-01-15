package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduleDispatcher extends Dispatcher {
    /**
     * 延迟调度事件分发
     */
    Future<?> scheduleDispatch(Object event, TimeUnit unit, long time, Object... params);

    /**
     * 定时调度事件分发
     */
    Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period, Object... params);
}
