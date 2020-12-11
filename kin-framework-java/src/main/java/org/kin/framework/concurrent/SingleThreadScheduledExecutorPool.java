package org.kin.framework.concurrent;

import org.kin.framework.utils.HashUtils;
import org.kin.framework.utils.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * {@link SingleThreadScheduledExecutor}池
 *
 * @author huangjianqin
 * @date 2020/11/23
 */
public class SingleThreadScheduledExecutorPool implements ScheduledExecutorService {
    //状态枚举
    private static final byte ST_STARTED = 1;
    private static final byte ST_SHUTDOWN = 2;
    private static final byte ST_TERMINATED = 3;

    /** 原子更新状态值 */
    private static final AtomicIntegerFieldUpdater<SingleThreadScheduledExecutorPool> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadScheduledExecutorPool.class, "state");

    /** 状态值 */
    private volatile int state = ST_STARTED;
    /** 线程锁, 用于关闭时阻塞 */
    private final CountDownLatch threadLock = new CountDownLatch(1);

    /** 线程数 */
    private final int coreSize;
    /** 绑定的线程池 */
    private final ExecutionContext executionContext;
    private final SingleThreadScheduledExecutor[] executors;

    public SingleThreadScheduledExecutorPool(int coreSize) {
        this(coreSize, StringUtils.firstLowerCase(SingleThreadScheduledExecutorPool.class.getSimpleName()));
    }

    public SingleThreadScheduledExecutorPool(int coreSize, String workerNamePrefix) {
        this.coreSize = coreSize;
        this.executionContext = ExecutionContext.fix(coreSize, workerNamePrefix);
        this.executors = new SingleThreadScheduledExecutor[coreSize];
        for (int i = 0; i < coreSize; i++) {
            executors[i] = new SingleThreadScheduledExecutor(executionContext);
        }
    }

    /**
     * 选择一个executor
     */
    private SingleThreadScheduledExecutor selectExecutor(Object task) {
        return executors[HashUtils.efficientHash(task, coreSize)];
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        return selectExecutor(command).schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        return selectExecutor(callable).schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        return selectExecutor(command).scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        return selectExecutor(command).scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {
        for (SingleThreadScheduledExecutor executor : executors) {
            executor.shutdown();
        }
        executionContext.shutdown();

        for (; ; ) {
            //等待所有Executor Shutdown
            boolean allShutdown = true;
            for (SingleThreadScheduledExecutor executor : executors) {
                if (!executor.isShutdown()) {
                    allShutdown = false;
                    break;
                }
            }

            if (allShutdown) {
                for (; ; ) {
                    int oldState = state;
                    if (oldState >= ST_SHUTDOWN || STATE_UPDATER.compareAndSet(
                            this, oldState, ST_SHUTDOWN)) {
                        break;
                    }
                }
                break;
            }
        }

        for (; ; ) {
            //等待所有Executor Terminated
            boolean allTerminated = true;
            for (SingleThreadScheduledExecutor executor : executors) {
                if (!executor.isTerminated()) {
                    allTerminated = false;
                    break;
                }
            }

            if (allTerminated) {
                break;
            }
        }
        STATE_UPDATER.set(this, ST_TERMINATED);
        threadLock.countDown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks = new ArrayList<>();
        for (SingleThreadScheduledExecutor executor : executors) {
            tasks.addAll(executor.shutdownNow());
        }
        return tasks;
    }

    @Override
    public boolean isShutdown() {
        return state >= ST_SHUTDOWN;
    }

    @Override
    public boolean isTerminated() {
        return state >= ST_TERMINATED;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        synchronized (this) {
            for (; ; ) {
                if (state >= ST_TERMINATED) {
                    return true;
                }
                if (nanos <= 0) {
                    return false;
                }

                threadLock.await(nanos, NANOSECONDS);
            }
        }
    }

    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        return selectExecutor(task).submit(task);
    }

    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        return selectExecutor(task).submit(task, result);
    }

    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        return selectExecutor(task).submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return selectExecutor(tasks).invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return selectExecutor(tasks).invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return selectExecutor(tasks).invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return selectExecutor(tasks).invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        selectExecutor(command).execute(command);
    }
}
