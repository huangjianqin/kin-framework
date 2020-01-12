package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledActorLikeDispatcher extends ActorLikeDispatcher {
    /**
     *
     */
    Future<?> scheduleDispatch(int partitionId, Object event, TimeUnit unit, long delay, Object... params);

    /**
     *
     */
    Future<?> scheduleDispatchAtFixRate(int partitionId, Object event, TimeUnit unit, long initialDelay, long period, Object... params);
}
