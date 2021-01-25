package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledPinnedThreadDispatcher extends PinnedThreadDispatcher {
    /**
     * 延迟调度事件分发
     * 该事件处理会在同一线程处理(根据 @param partitionId 区分)
     *
     * @param partitionId 分区id
     * @param event       事件
     * @param unit        时间单位
     * @param delay       延迟执行事件
     */
    Future<?> scheduleDispatch(int partitionId, Object event, TimeUnit unit, long delay);

    /**
     * 定时调度事件分发
     * 该事件处理会在同一线程处理(根据 @param partitionId 区分)
     *
     * @param partitionId  分区id
     * @param event        事件
     * @param unit         时间单位
     * @param initialDelay 延迟执行事件
     * @param period       事件间隔
     */
    Future<?> scheduleDispatchAtFixRate(int partitionId, Object event, TimeUnit unit, long initialDelay, long period);
}
