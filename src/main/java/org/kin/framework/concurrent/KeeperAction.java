package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2019/7/10
 */
public interface KeeperAction {
    /**
     * 执行keeper逻辑前预处理
     */
    void preAction();

    /**
     * 执行keeper逻辑
     */
    void action();

    /**
     * 执行keeper逻辑后的逻辑处理
     */
    void postAction();
}
