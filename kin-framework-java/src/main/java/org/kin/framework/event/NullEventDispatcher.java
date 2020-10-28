package org.kin.framework.event;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface NullEventDispatcher {
    /**
     * 直接运行一个任务
     *
     * @param runnable 消息逻辑
     */
    void dispatch(Runnable runnable);
}
