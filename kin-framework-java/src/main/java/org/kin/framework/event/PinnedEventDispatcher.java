package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * actor风格的事件分发
 * 支持事务顺序处理事件
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public class PinnedEventDispatcher extends ParallelEventDispatcher implements PinnedScheduledDispatcher {

    public PinnedEventDispatcher(int parallelism) {
        super(parallelism);
    }

    public PinnedEventDispatcher(int parallelism, boolean isEnhance) {
        super(parallelism, isEnhance);
    }

    @Override
    public void dispatch(int partitionId, Object event, Object... params) {
        dispatch(partitionId, event, EventCallback.EMPTY, params);
    }

    @Override
    public void dispatch(int partitionId, Object event, EventCallback callback, Object... params) {
        dispatch(new EventContext(partitionId, event, params, callback));
    }

    @Override
    public Future<?> scheduleDispatch(int partitionId, Object event, TimeUnit unit, long delay, Object... params) {
        return scheduledExecutors.schedule(() -> dispatch(partitionId, event, params), delay, unit);
    }

    @Override
    public Future<?> scheduleDispatchAtFixRate(int partitionId, Object event, TimeUnit unit, long initialDelay, long period, Object... params) {
        return scheduledExecutors.scheduleAtFixedRate(() -> dispatch(partitionId, event, params), initialDelay, period, unit);
    }
}
