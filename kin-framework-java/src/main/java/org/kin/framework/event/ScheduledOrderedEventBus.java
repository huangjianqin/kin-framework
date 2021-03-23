package org.kin.framework.event;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ScheduledOrderedEventBus extends OrderedEventBus {
    /**
     * 延迟调度事件分发
     *
     * @param partitionId 分区id
     * @param unit        时间单位
     * @param delay       延迟执行事件
     * @param unit        时间单位
     */
    Future<?> schedule(int partitionId, Object event, long delay, TimeUnit unit);

    /**
     * 定时时间间隔调度事件分发
     *
     * @param partitionId  分区id
     * @param unit         时间单位
     * @param initialDelay 延迟执行时间
     * @param period       时间间隔
     * @param unit         时间单位
     */
    Future<?> scheduleAtFixRate(int partitionId, Object event, long initialDelay, long period, TimeUnit unit);

    /**
     * 固定时间延迟调度事件分发
     *
     * @param partitionId  分区id
     * @param event        事件
     * @param initialDelay 延迟执行时间
     * @param delay        时间延迟
     * @param unit         时间单位
     */
    Future<?> scheduleWithFixedDelay(int partitionId, Object event, long initialDelay, long delay, TimeUnit unit);
}
