package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledPinnedThreadEventDispatcher extends PinnedThreadEventDispatcher {
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
     * 定时时间间隔调度事件分发
     * 该事件处理会在同一线程处理(根据 @param partitionId 区分)
     *
     * @param partitionId  分区id
     * @param event        事件
     * @param unit         时间单位
     * @param initialDelay 延迟执行时间
     * @param period       时间间隔
     */
    Future<?> scheduleDispatchAtFixRate(int partitionId, Object event, TimeUnit unit, long initialDelay, long period);

    /**
     * 固定时间延迟调度事件分发
     * 该事件处理会在同一线程处理(根据 @param partitionId 区分)
     *
     * @param partitionId  分区id
     * @param event        事件
     * @param unit         时间单位
     * @param initialDelay 延迟执行时间
     * @param delay        时间延迟
     */
    Future<?> scheduleDispatchWithFixedDelay(int partitionId, Object event, TimeUnit unit, long initialDelay, long delay);
}
