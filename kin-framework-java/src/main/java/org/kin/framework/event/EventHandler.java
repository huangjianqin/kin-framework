package org.kin.framework.event;

/**
 * 事件处理器接口
 *
 * @author 健勤
 * @date 2017/8/8
 */
@FunctionalInterface
public interface EventHandler<T extends AbstractEvent<?>> {
    /**
     * 事件处理逻辑
     *
     * @param event 事件实现类
     */
    void handle(T event);
}
