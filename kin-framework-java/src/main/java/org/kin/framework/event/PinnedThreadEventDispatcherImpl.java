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
public class PinnedThreadEventDispatcherImpl extends ParallelEventDispatcher implements ScheduledPinnedThreadEventDispatcher {

    public PinnedThreadEventDispatcherImpl(int parallelism) {
        super(parallelism);
    }

    public PinnedThreadEventDispatcherImpl(int parallelism, boolean isEnhance) {
        super(parallelism, isEnhance);
    }

    @Override
    public void dispatch(int partitionId, Object event) {
        dispatch(partitionId, event, EventCallback.EMPTY);
    }

    @Override
    public void dispatch(int partitionId, Object event, EventCallback callback) {
        dispatch(new EventContext(partitionId, event, callback));
    }

    @Override
    public Future<?> scheduleDispatch(int partitionId, Object event, TimeUnit unit, long delay) {
        return executor.schedule(partitionId, () -> dispatch0(partitionId, event), delay, unit);
    }

    @Override
    public Future<?> scheduleDispatchAtFixRate(int partitionId, Object event, TimeUnit unit, long initialDelay, long period) {
        return executor.scheduleAtFixedRate(partitionId, () -> dispatch0(partitionId, event), initialDelay, period, unit);
    }

    @Override
    public Future<?> scheduleDispatchWithFixedDelay(int partitionId, Object event, TimeUnit unit, long initialDelay, long delay) {
        return executor.scheduleWithFixedDelay(partitionId, () -> dispatch0(partitionId, event), initialDelay, delay, unit);
    }
}
