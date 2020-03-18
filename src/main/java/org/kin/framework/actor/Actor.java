package org.kin.framework.actor;


import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2018/2/26
 * <p>
 * Actor行为抽象
 * Actor的行为分派调度由具体的业务系统自身定义
 */
public interface Actor<A extends Actor<?>> {
    /**
     * 消息匹配对应预定义方法并执行
     *
     * @param message 消息
     */
    <T> void receive(T message);

    /**
     * 执行@message 方法
     *
     * @param message 消息
     */
    void tell(Message<A> message);

    /**
     * 调度执行@message 方法
     *
     * @param message 消息
     * @param delay   延迟执行时间
     * @param unit    时间单位
     */
    Future<?> schedule(Message<A> message, long delay, TimeUnit unit);

    /**
     * 周期性调度执行@message 方法
     *
     * @param message      消息
     * @param initialDelay 延迟执行时间
     * @param period       时间间隔
     * @param unit         时间单位
     */
    Future<?> scheduleAtFixedRate(Message<A> message, long initialDelay, long period, TimeUnit unit);

    /**
     * 相当于receive PoisonPill.instance()
     * Actor 线程执行
     * 待mailbox里面的mail执行完, 关闭Actor, 并释放资源
     */
    void stop();

    /**
     * 非Actor 线程执行
     * 直接中断 Actor 线程, 关闭Actor, 并释放资源
     */
    void stopNow();
}
