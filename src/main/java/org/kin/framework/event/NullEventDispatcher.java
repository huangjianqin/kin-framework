package org.kin.framework.event;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface NullEventDispatcher {
    void dispatch(Runnable runnable);
}
