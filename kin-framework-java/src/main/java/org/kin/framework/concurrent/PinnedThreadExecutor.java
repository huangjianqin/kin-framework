package org.kin.framework.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 推荐使用继承实现
 * 每个实例绑定一个线程处理消息, 支持阻塞(当然线程池需要足够大)
 * 这里保证的是message在同一线程下处理, 但不保证每次处理都是同一条线程, 即ThreadLocal是不可用的
 *
 * @author huangjianqin
 * @date 2019/7/9
 */
public class PinnedThreadExecutor<TS extends PinnedThreadExecutor<TS>> implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PinnedThreadExecutor.class);

    /** 线程池 */
    private final ExecutionContext executionContext;
    /** 消息队列 */
    private final Queue<Message<TS>> inBox = new LinkedBlockingQueue<>();
    /** 消息数量 */
    private final AtomicInteger boxSize = new AtomicInteger();
    /** 当前占用线程 */
    private volatile Thread currentThread;
    /** PinnedThreadSafeHandler是否已关闭 */
    private volatile boolean isStopped = false;

    public PinnedThreadExecutor(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * 处理消息
     */
    public void handle(Message<TS> message) {
        if (!isStopped) {
            inBox.add(message);
            tryRun();
        }
    }

    /**
     * 调度处理消息
     */
    public Future<?> schedule(Message<TS> message, long delay, TimeUnit unit) {
        if (!isStopped) {
            return executionContext.schedule(() -> handle(message), delay, unit);
        }
        return null;
    }

    /**
     * 固定事件间隔处理消息
     */
    public Future<?> scheduleAtFixedRate(Message<TS> message, long initialDelay, long period, TimeUnit unit) {
        if (!isStopped) {
            return executionContext.scheduleAtFixedRate(() -> handle(message), initialDelay, period, unit);
        }
        return null;
    }

    public void stop() {
        if (!isStopped) {
            isStopped = true;
        }
    }

    /**
     * @return 是否在同一线程loop
     */
    public boolean isInLoop() {
        if (Objects.nonNull(this.currentThread)) {
            return this.currentThread == Thread.currentThread();
        }

        return false;
    }

    /**
     * 消息处理实现逻辑
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        this.currentThread = Thread.currentThread();
        while (!isStopped && !this.currentThread.isInterrupted()) {
            Message<TS> message = inBox.poll();
            if (message == null) {
                break;
            }

            long st = System.currentTimeMillis();
            try {
                message.handle((TS) this);
            } catch (Exception e) {
                log.error("", e);
            }
            long cost = System.currentTimeMillis() - st;

            if (cost >= getWarnMsgCostTime()) {
                log.warn("handle message({}) cost {} ms", message, cost);
            }

            if (boxSize.decrementAndGet() <= 0) {
                break;
            }
        }
        this.currentThread = null;
    }

    /**
     * 尝试绑定线程, 并执行消息处理
     */
    private void tryRun() {
        if (!isStopped && boxSize.incrementAndGet() == 1) {
            executionContext.execute(this);
        }
    }

    /**
     * @return 每条消息处理时间上限, 如果超过该上限就会打warn日志
     */
    protected int getWarnMsgCostTime() {
        return 200;
    }
}
