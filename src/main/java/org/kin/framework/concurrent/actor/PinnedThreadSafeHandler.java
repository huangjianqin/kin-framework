package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2019/7/9
 * <p>
 * 推荐使用继承实现
 * 每个实例绑定一个线程处理消息, 支持阻塞(线程池需要足够大)
 */
public class PinnedThreadSafeHandler<TS extends PinnedThreadSafeHandler<TS>> implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(PinnedThreadSafeHandler.class);

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

    public PinnedThreadSafeHandler(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        PinnedThreadSafeFuturesManager.instance().register(this);
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
            Future future = executionContext.schedule(() -> handle(message), delay, unit);
            PinnedThreadSafeFuturesManager.instance().addFuture(this, future);
            return future;
        }
        return null;
    }

    /**
     * 固定事件间隔处理消息
     */
    public Future<?> scheduleAtFixedRate(Message<TS> message, long initialDelay, long period, TimeUnit unit) {
        if (!isStopped) {
            Future future = executionContext.scheduleAtFixedRate(() -> handle(message), initialDelay, period, unit);
            PinnedThreadSafeFuturesManager.instance().addFuture(this, future);
            return future;
        }
        return null;
    }

    public void stop() {
        if (!isStopped) {
            isStopped = true;
            PinnedThreadSafeFuturesManager.instance().unRegister(this);
        }
    }

    /**
     * 消息处理实现逻辑
     */
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
                ExceptionUtils.log(e);
            }
            long cost = System.currentTimeMillis() - st;

            if (cost >= getWarnMsgCostTime()) {
                log.warn("handle mail({}) cost {} ms", message, cost);
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
