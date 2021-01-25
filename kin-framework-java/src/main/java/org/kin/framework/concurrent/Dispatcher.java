package org.kin.framework.concurrent;

import org.kin.framework.Closeable;

import java.util.concurrent.TimeUnit;

/**
 * 根据指定KEY, 分派消息
 *
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
     * 判断Receiver是否已注册
     *
     * @param key Receiver标识
     * @return 是否已注册
     */
    boolean isRegistered(KEY key);

    /**
     * 推送消息
     *
     * @param key     Receiver标识
     * @param message 消息实现
     */
    void postMessage(KEY key, MSG message);

    /**
     * 向所有已注册Receiver推送消息
     */
    void post2All(MSG message);

    /**
     * 延迟调度推送消息
     *
     * @param key     Receiver标识
     * @param message 消息实现
     * @param delay   延迟时间
     * @param unit    延迟时间单位
     */
    void schedule(KEY key, MSG message, long delay, TimeUnit unit);

    /**
     * 固定间隔调度推送消息
     *
     * @param key          Receiver标识
     * @param message      消息实现
     * @param initialDelay 首次延迟时间
     * @param period       固定间隔
     * @param unit         间隔时间单位
     */
    void scheduleAtFixedRate(KEY key, MSG message, long initialDelay, long period, TimeUnit unit);

    /**
     * 固定延迟调度推送消息
     *
     * @param key          Receiver标识
     * @param message      消息实现
     * @param initialDelay 首次延迟时间
     * @param delay        固定延迟
     * @param unit         延迟时间单位
     */
    void scheduleWithFixedDelay(KEY key, MSG message, long initialDelay, long delay, TimeUnit unit);

    /**
     * 关闭Dispatcher
     */
    void shutdown();

    /**
     * 获取底层执行dispatch逻辑的executors
     */
    ExecutionContext executionContext();
}
