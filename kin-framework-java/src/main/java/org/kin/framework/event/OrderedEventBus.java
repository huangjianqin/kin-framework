package org.kin.framework.event;

/**
 * 支持事件有序处理
 * <p>
 * 目前实现是基于{@link org.kin.framework.concurrent.DefaultPartitionExecutor}, 其底层是基于{@link org.kin.framework.concurrent.FixOrderedEventLoopGroup}
 * 所以, 事件会在同一线程有序处理(根据 @param partitionId 区分)
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface OrderedEventBus extends EventBus {
    /**
     * 分发事件
     *
     * @param partitionId 分区
     * @param event       事件实例
     */
    void post(int partitionId, Object event);
}
