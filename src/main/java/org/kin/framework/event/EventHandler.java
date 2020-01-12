package org.kin.framework.event;

/**
 * Created by 健勤 on 2017/8/8.
 * 事件处理器接口
 */
@FunctionalInterface
public interface EventHandler<T extends AbstractEvent> {
    void handle(T event);
}
