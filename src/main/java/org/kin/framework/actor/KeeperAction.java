package org.kin.framework.actor;

/**
 * @author huangjianqin
 * @date 2019/7/10
 */
public interface KeeperAction {
    void preAction();
    void action();
    void postAction();
}
