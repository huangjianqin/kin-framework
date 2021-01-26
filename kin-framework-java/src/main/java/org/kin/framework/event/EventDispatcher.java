package org.kin.framework.event;

import java.lang.reflect.Method;

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
     * @param eventClass 事件类
     * @param handler    事件处理实例
     * @param method     事件处理方法
     */
    void register(Class<?> eventClass, Object handler, Method method);

    /**
     * 分发事件
     *
     * @param event 事件实例
     */
    void dispatch(Object event);

    /**
     * 分发事件
     *
     * @param event    事件实例
     * @param callback callback回调
     */
    void dispatch(Object event, EventCallback callback);

    /**
     * shutdown
     */
    void shutdown();
}
