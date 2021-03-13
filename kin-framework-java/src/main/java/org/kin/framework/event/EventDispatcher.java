package org.kin.framework.event;

/**
 * 事件分发接口
 *
 * @author 健勤
 * @date 2017/8/8
 */
public interface EventDispatcher {
    /**
     * 注册事件处理器
     *
     * @param eventClass   事件类, 目前事件类最好比较native, 也就是不带泛型的, 也最好不是集合类, 数组等等
     * @param eventHandler {@link EventHandler}实现类
     */
    <T> void register(Class<T> eventClass, EventHandler<T> eventHandler);

    /**
     * 分发事件
     *
     * @param event 事件实例
     */
    void dispatch(Object event);

    /**
     * shutdown
     */
    void shutdown();
}
