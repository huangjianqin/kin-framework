package org.kin.framework.event;

/**
 * 事件总线接口
 *
 * @author 健勤
 * @date 2017/8/8
 */
public interface EventBus {
    /**
     * 注册事件处理器
     *
     * @param obj {@link EventHandler}实现类或者public方法带有{@link EventFunction} 注解的实例
     */
    void register(Object obj);

    /**
     * 分发事件
     *
     * @param event 事件实例
     */
    void post(Object event);

    /**
     * shutdown
     */
    void shutdown();
}
