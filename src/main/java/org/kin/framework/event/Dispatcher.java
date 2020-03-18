package org.kin.framework.event;

import java.lang.reflect.Method;

/**
 * @author 健勤
 * @date 2017/8/8
 * 事件分发接口
 */
public interface Dispatcher {
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
     * @param event  事件实例
     * @param params 额外参数
     */
    void dispatch(Object event, Object... params);

    /**
     * 分发事件
     *
     * @param event    事件实例
     * @param callback callback回调
     * @param params   额外参数
     */
    void dispatch(Object event, EventCallback callback, Object... params);
}
