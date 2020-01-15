package org.kin.framework.event;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ActorLikeDispatcher {
    /**
     * 注册事件处理器
     */
    void register(Class<?> eventClass, Object handler, Method method);

    /**
     * 分发事件
     */
    void dispatch(int partitionId, Object event, Object... params);

    /**
     * 分发事件
     */
    void dispatch(int partitionId, Object event, EventCallback callback, Object... params);

    /**
     * 异步分发事件
     */
    void asyncDispatch(int partitionId, Object event, Object... params);

    /**
     * 异步分发事件
     */
    void asyncDispatch(int partitionId, Object event, EventCallback callback, Object... params);
}
