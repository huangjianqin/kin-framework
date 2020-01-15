package org.kin.framework.event;

import java.lang.reflect.Method;

/**
 * Created by 健勤 on 2017/8/8.
 * 事件分发接口
 */
public interface Dispatcher {
    /**
     * 注册事件处理器
     */
    void register(Class<?> eventClass, Object handler, Method method);

    /**
     * 分发事件
     */
    void dispatch(Object event, Object... params);

    /**
     * 异步分发事件
     */
    void asyncDispatch(Object event, Object... params);

    /**
     * 异步分发事件
     */
    void asyncDispatch(Object event, EventCallback callback, Object... params);
}
