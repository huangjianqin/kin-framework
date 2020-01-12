package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduleDispatcher extends Dispatcher {
    /**
     *
     */
    Future<?> scheduleDispatch(Object event, TimeUnit unit, long time, Object... params);

    /**
     *
     */
    Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period, Object... params);
}
