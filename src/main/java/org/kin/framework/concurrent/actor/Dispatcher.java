package org.kin.framework.concurrent.actor;

import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ExecutionContext;

/**
 * @author huangjianqin
 * @date 2020-04-26
 */
public interface Dispatcher<KEY, MSG> extends Closeable {
    /**
     * 注册Receiver
     *
     * @param key              Receiver标识
     * @param receiver         Receiver实现
     * @param enableConcurrent 是否允许并发执行
     */
    void register(KEY key, Receiver<MSG> receiver, boolean enableConcurrent);

    /**
     * 注销Receiver
     * @param key Receiver标识
     */
    void unregister(KEY key);

    /**
     * 处理消息
     * @param key Receiver标识
     * @param message 消息实现
     */
    void postMessage(KEY key, MSG message);

    /**
     * 关闭Dispatcher
     */
    void shutdown();

    /**
     * 获取底层执行dispatch逻辑的executors
     *
     * @return
     */
    ExecutionContext executionContext();
}
