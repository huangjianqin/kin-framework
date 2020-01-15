package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledActorLikeDispatcher extends ActorLikeDispatcher {
    /**
     * 延迟调度事件分发
     * 该事件处理会在同一线程处理(根据 @param partitionId 区分)
     */
    Future<?> scheduleDispatch(int partitionId, Object event, TimeUnit unit, long delay, Object... params);

    /**
     * 定时调度事件分发
     * 该事件处理会在同一线程处理(根据 @param partitionId 区分)
     */
    Future<?> scheduleDispatchAtFixRate(int partitionId, Object event, TimeUnit unit, long initialDelay, long period, Object... params);
}
