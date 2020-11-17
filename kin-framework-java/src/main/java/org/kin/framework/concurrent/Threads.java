package org.kin.framework.concurrent;

/**
 * 线程工具类
 *
 * @author huangjianqin
 * @date 2020/11/17
 */
public class Threads {
    /**
     * 获取ThreadGroup
     */
    public static ThreadGroup getThreadGroup() {
        SecurityManager local = System.getSecurityManager();
        return local == null ? Thread.currentThread().getThreadGroup() : local.getThreadGroup();
    }
}
