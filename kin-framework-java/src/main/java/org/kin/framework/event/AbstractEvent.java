package org.kin.framework.event;

/**
 * @author 健勤
 * @date 2017/8/8
 * 事件的抽象
 */
public abstract class AbstractEvent<TYPE extends Enum<TYPE>> {
    private final TYPE type;
    private final long timestamp;

    public AbstractEvent(TYPE type) {
        this.type = type;
        timestamp = System.currentTimeMillis();
    }

    public TYPE getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "EventType:" + type;
    }
}
