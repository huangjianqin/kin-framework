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
     * 根据事件类型分发给对应的事件处理器
     */
    void dispatch(int partitionId, Object event, Object... params);

    /**
     * 根据事件类型分发给对应的事件处理器
     * 异步
     */
    void asyncDispatch(int partitionId, Object event, Object... params);
}
