package org.kin.framework.service;

/**
 * 服务状态改变触发的监听器
 *
 * @author huangjianqin
 * @date 2017/8/8
 */
@FunctionalInterface
public interface ServiceStateChangeListener<S extends Service> {
    /**
     * @param service 服务实例
     * @param pre     服务之前的状态
     */
    void onStateChanged(S service, Service.State pre);
}
