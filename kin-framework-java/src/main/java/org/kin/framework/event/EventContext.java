package org.kin.framework.event;

/**
 * 事件上下文
 *
 * @author huangjianqin
 * @date 2021/3/13
 */
class EventContext {
    /** 分区id */
    private final int partitionId;
    /** 事件 */
    private final Object event;

    EventContext(Object event) {
        this(event.getClass().hashCode(), event);
    }

    EventContext(int partitionId, Object event) {
        this.partitionId = partitionId;
        this.event = event;
    }

    //getter
    int getPartitionId() {
        return partitionId;
    }

    Object getEvent() {
        return event;
    }
}
