package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2019/7/10
 */
public interface KeeperAction {
    void preAction();

    void action();

    void postAction();
}
