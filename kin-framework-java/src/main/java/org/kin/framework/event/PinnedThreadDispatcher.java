package org.kin.framework.event;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface PinnedThreadDispatcher {
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
     * @param partitionId 分区
     * @param event       事件实例
     */
    void dispatch(int partitionId, Object event);

    /**
     * 分发事件
     *
     * @param partitionId 分区
     * @param event       事件实例
     * @param callback    callback回调
     */
    void dispatch(int partitionId, Object event, EventCallback callback);
}
