package org.kin.framework.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 推荐使用继承实现
 * 每个实例绑定一个线程处理消息, 支持阻塞(当然线程池需要足够大)
 * 这里保证的是message在同一线程下处理, 但不保证每次处理都是同一条线程, 即{@link ThreadLocal}是不可用的
 * <p>
 * 与{@link SingleThreadEventLoop}最大区别是{@link OrderedEventLoop}保证消息有序执行, 但不保证在同一线程执行,
 * 而{@link SingleThreadEventLoop}不仅仅保证消息有序执行, 还保证在同一线程执行. 所以{@link SingleThreadEventLoop}处理的消息逻辑,
 * 不建议会阻塞或者IO等比较耗时的操作, 而{@link OrderedEventLoop}允许这些操作, 但过大这些操作带有的后果就是可能创建过多线程, 并占用大量资源
 *
 * @author huangjianqin
 * @date 2019/7/9
 */
public class OrderedEventLoop<P extends OrderedEventLoop<P>> implements EventLoop<P> {
    private static final Logger log = LoggerFactory.getLogger(OrderedEventLoop.class);

    private final EventLoopGroup<P> eventLoopGroup;
    /** 线程池 */
    private final ExecutionContext executionContext;
    /** 消息队列 */
    private final Queue<Message<P>> inBox = new LinkedBlockingQueue<>();
    /** 消息数量 */
    private final AtomicInteger boxSize = new AtomicInteger();
    /** 是否已关闭 */
    private volatile boolean stopped = false;
    /** 内置Runnable */
    private final Loop loop = new Loop();

    public OrderedEventLoop(EventLoopGroup<P> eventLoopGroup, ExecutionContext executionContext) {
        this.eventLoopGroup = eventLoopGroup;
        this.executionContext = executionContext;
    }

    @Override
    public EventLoopGroup<P> parent() {
        return eventLoopGroup;
    }

    /**
     * 接收消息
     */
    @Override
    public final void receive(Message<P> message) {
        if (!isShutdown()) {
            inBox.add(message);
            tryRun();
        }
    }

    /**
     * 调度处理消息
     */
    @Override
    public final ScheduledFuture<?> schedule(Message<P> message, long delay, TimeUnit unit) {
        if (!isShutdown()) {
            return executionContext.schedule(() -> receive(message), delay, unit);
        }
        throw new IllegalStateException("executor is stopped");
    }

    /**
     * 固定速率处理消息
     */
    @Override
    public final ScheduledFuture<?> scheduleAtFixedRate(Message<P> message, long initialDelay, long period, TimeUnit unit) {
        if (!isShutdown()) {
            return executionContext.scheduleAtFixedRate(() -> receive(message), initialDelay, period, unit);
        }
        throw new IllegalStateException("executor is stopped");
    }

    /**
     * 固定延迟处理消息
     */
    @Override
    public final ScheduledFuture<?> scheduleWithFixedDelay(Message<P> message, long initialDelay, long period, TimeUnit unit) {
        if (!isShutdown()) {
            return executionContext.scheduleWithFixedDelay(() -> receive(message), initialDelay, period, unit);
        }
        throw new IllegalStateException("executor is stopped");
    }

    /**
     * 尝试绑定线程, 并执行消息处理
     */
    private void tryRun() {
        if (!isShutdown() && boxSize.incrementAndGet() == 1) {
            executionContext.execute(loop);
        }
    }

    /**
     * @return 每条消息处理时间上限, 如果超过该上限就会打warn日志
     */
    protected int getWarnMsgCostTime() {
        return 200;
    }

    @Override
    public boolean isInEventLoop(Thread thread) {
        if (isShutdown() && Objects.nonNull(loop.currentThread)) {
            return loop.currentThread == Thread.currentThread();
        }

        return false;
    }

    @Override
    public void shutdown() {
        if (!isShutdown() && Objects.nonNull(eventLoopGroup) && eventLoopGroup instanceof FixOrderedEventLoopGroup) {
            //FixOrderedEventLoopPool 不支持单独shutdown OrderedEventLoop
            stopped = true;
        }
    }

    @Override
    public boolean isShutdown() {
        return stopped;
    }

    @Override
    public boolean isTerminated() {
        return stopped;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        return schedule(runnable2Message(command), delay, unit);
    }

    @Deprecated
    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        return scheduleAtFixedRate(runnable2Message(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        return scheduleWithFixedDelay(runnable2Message(command), initialDelay, delay, unit);
    }

    @Deprecated
    @Override
    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        receive(runnable2Message(command));
    }

    //------------------------------------------------------------------------------------------------------------------------------
    private class Loop implements Runnable {
        /** 当前占用线程, 因为存在线程问题, 不能set null */
        private volatile Thread currentThread;

        @SuppressWarnings("unchecked")
        @Override
        public final void run() {
            currentThread = Thread.currentThread();
            while (!isShutdown() && !currentThread.isInterrupted()) {
                Message<P> message = inBox.poll();
                if (message == null) {
                    break;
                }

                long st = System.currentTimeMillis();
                try {
                    message.handle((P) OrderedEventLoop.this);
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
        }
    }
}
