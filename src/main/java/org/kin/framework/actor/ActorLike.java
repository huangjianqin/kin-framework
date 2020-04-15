package org.kin.framework.actor;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2019/7/9
 * <p>
 * 推荐使用继承实现
 */
public class ActorLike<AL extends ActorLike<?>> implements Actor<AL>, Runnable {
    private static final Logger log = LoggerFactory.getLogger(ActorLike.class);

    private static Map<ActorLike<?>, Queue<Future>> FUTURES = new ConcurrentHashMap<>();

    private ExecutionContext executionContext;
    private final Queue<Message<AL>> inBox = new LinkedBlockingQueue<>();
    private final AtomicInteger boxSize = new AtomicInteger();
    private volatile Thread currentThread;
    private volatile boolean isStopped = false;

    protected ActorLike(ExecutionContext executionContext) {
        this.executionContext = executionContext;
        //每1h清理已结束的调度
        scheduleAtFixedRate(actor -> clearFinishedFutures(), 0, 1, TimeUnit.HOURS);

        JvmCloseCleaner.DEFAULT().add(executionContext::shutdown);
    }

    public ActorLike(ExecutorService executorService, int scheduleCoreNum, ThreadFactory scheduleThreadFactory) {
        this(new ExecutionContext(executorService, scheduleCoreNum, scheduleThreadFactory));
    }

    @Override
    public void tell(Message<AL> message) {
        if (!isStopped) {
            inBox.add(message);
            tryRun();
        }
    }

    @Override
    public Future<?> schedule(Message<AL> message, long delay, TimeUnit unit) {
        if (!isStopped) {
            Future future = executionContext.schedule(() -> tell(message), delay, unit);
            addFuture(future);
            return future;
        }
        return null;
    }

    @Override
    public Future<?> scheduleAtFixedRate(Message<AL> message, long initialDelay, long period, TimeUnit unit) {
        if (!isStopped) {
            Future future = executionContext.scheduleAtFixedRate(() -> tell(message), initialDelay, period, unit);
            addFuture(future);
            return future;
        }
        return null;
    }

    @Override
    public void stop() {
        if (!isStopped) {
            isStopped = true;
            clearFutures();
        }
    }

    @Override
    public void run() {
        this.currentThread = Thread.currentThread();
        while (!isStopped && !this.currentThread.isInterrupted()) {
            Message message = inBox.poll();
            if (message == null) {
                break;
            }

            long st = System.currentTimeMillis();
            try {
                message.handle(this);
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

    private void tryRun() {
        if (!isStopped && boxSize.incrementAndGet() == 1) {
            executionContext.execute(this);
        }
    }

    private void addFuture(Future<?> future) {
        Queue<Future> queue;
        while ((queue = FUTURES.putIfAbsent(this, new ConcurrentLinkedQueue<>())) == null) {
        }
        queue.add(future);
    }

    private void clearFutures() {
        Queue<Future> old = FUTURES.remove(this);
        if (old != null) {
            for (Future future : old) {
                if (!future.isDone() || !future.isCancelled()) {
                    future.cancel(true);
                }
            }
        }
    }

    private void clearFinishedFutures() {
        Queue<Future> old = FUTURES.get(this);
        if (old != null) {
            old.removeIf(Future::isDone);
        }
    }

    /**
     * @return 每条消息处理时间上限, 如果超过该上限就会打warn日志
     */
    protected int getWarnMsgCostTime() {
        return 200;
    }
}
