package org.kin.framework;

/**
 * @author huangjianqin
 * @date 2019/2/28
 */
public interface Closeable {
    /**
     * close服务并释放资源
     */
    void close();

    /**
     * 默认方法, 绑定jvm shutdownhook close服务并释放资源
     */
    default void monitorJVMClose() {
        JvmCloseCleaner.instance().add(this);
    }
}
