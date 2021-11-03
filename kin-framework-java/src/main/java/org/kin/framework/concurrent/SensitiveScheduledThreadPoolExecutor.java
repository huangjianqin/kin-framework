package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.Closeable;
import org.kin.framework.utils.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * 支持时间敏感的调度器
 *
 * @author huangjianqin
 * @date 2021/6/5
 */
public class SensitiveScheduledThreadPoolExecutor implements ScheduledExecutorService, Closeable {
    //状态枚举
    /** not start */
    private static final byte ST_NOT_STARTED = 1;
    /** started */
    private static final byte ST_STARTED = 2;
    /** shutting down */
    private static final byte ST_SHUTTING_DOWN = 3;
    /** shutdown */
    private static final byte ST_SHUTDOWN = 4;
    /** terminated */
    private static final byte ST_TERMINATED = 5;
    /** 原子更新状态值 */
    private static final AtomicIntegerFieldUpdater<SensitiveScheduledThreadPoolExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SensitiveScheduledThreadPoolExecutor.class, "state");
    /** 时间变化检查间隔,耗秒 */
    private static final int CHECK_INTERNAL = 3_000;
    /** 最大允许时间变化为10s */
    private static final int MAX_CHANGE_DURATION = 10_000;

    /** 是否时间敏感(也就是随系统时间发生变化而变化), 则TimeUnit.MILLISECONDS, 否则是TimeUnit.NANOSECONDS */
    private final TimeUnit timeUnit;
    /** 实例创建时间 */
    private final long createTime = now();
    /** 内置实现队列 */
    private final RefreshableDelayQueue<ScheduledFutureTask<?>> queue = new RefreshableDelayQueue<>();
    /** 调度处理的executor */
    private final ExecutionContext executor;
    /** 线程锁, 用于关闭时阻塞 */
    private final CountDownLatch threadLock = new CountDownLatch(1);
    /** 状态值 */
    private volatile int state = ST_NOT_STARTED;

    public SensitiveScheduledThreadPoolExecutor(int corePoolSize) {
        this(corePoolSize, false);
    }

    public SensitiveScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        this(corePoolSize, false, threadFactory);
    }

    public SensitiveScheduledThreadPoolExecutor(int corePoolSize, boolean timeSensitive) {
        this(corePoolSize, timeSensitive, null);
    }

    public SensitiveScheduledThreadPoolExecutor(int corePoolSize, boolean timeSensitive, ThreadFactory threadFactory) {
        Preconditions.checkArgument(corePoolSize > 0, "core thread num must be greater than 0");

        if (Objects.isNull(threadFactory)) {
            threadFactory = new SimpleThreadFactory("sensitive-scheduler");
        }

        this.executor = ExecutionContext.fix(corePoolSize + 1, threadFactory);

        if (timeSensitive) {
            timeUnit = TimeUnit.MILLISECONDS;
            timeChangeCheck();
        } else {
            timeUnit = TimeUnit.NANOSECONDS;
        }

        executor.execute(this::loop);
    }

    /**
     * 一条线程作为loop, 循环获取task, 然后交给线程池其余线程处理
     */
    private void loop() {
        while (!isShutdown()) {
            try {
                ScheduledFutureTask<?> task = queue.take();
                executor.execute(task);
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    /**
     * 时间变化检查
     */
    private void timeChangeCheck() {
        //时间变化检查
        long now = now();
        scheduleWithFixedDelay(() -> {
            if (now() - now >= MAX_CHANGE_DURATION) {
                queue.refresh();
            }
            timeChangeCheck();
        }, CHECK_INTERNAL, CHECK_INTERNAL, SECONDS);
    }

    /**
     * 获取当前时间
     */
    private long now() {
        if (TimeUnit.MILLISECONDS.equals(timeUnit)) {
            return System.currentTimeMillis();
        } else {
            return System.nanoTime();
        }
    }

    /**
     * 用该时间作为now基准, 以提高数值运算效率
     *
     * @return 时间间隔
     */
    private long interval() {
        return now() - createTime;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> task = new ScheduledFutureTask<>(command, timeUnit.convert(delay, unit));
        delayedExecute(task);
        return task;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(callable, "task is null");

        ScheduledFutureTask<V> task = new ScheduledFutureTask<>(callable, timeUnit.convert(delay, unit));
        delayedExecute(task);
        return task;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> task = new ScheduledFutureTask<>(command, timeUnit.convert(initialDelay, unit), timeUnit.convert(period, unit));
        delayedExecute(task);
        return task;

    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> task = new ScheduledFutureTask<>(command, timeUnit.convert(initialDelay, unit), -timeUnit.convert(delay, unit));
        delayedExecute(task);
        return task;

    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    public void shutdown() {
        synchronized (this) {
            if (state >= ST_SHUTTING_DOWN) {
                //已结束
                return;
            }
            if (state < ST_STARTED) {
                //未开始
                STATE_UPDATER.set(this, ST_TERMINATED);
            }
            executor.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> taskList;
        synchronized (this) {
            shutdown();
            taskList = new ArrayList<>(Arrays.asList(queue.toArray(new Runnable[0])));
        }
        return taskList;
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
        long realTimeout = timeUnit.convert(timeout, unit);
        synchronized (this) {
            for (; ; ) {
                if (state >= ST_TERMINATED) {
                    return true;
                }
                if (realTimeout <= 0) {
                    return false;
                }

                threadLock.await(realTimeout, timeUnit);
            }
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Preconditions.checkNotNull(task, "task is null");
        ScheduledFutureTask<T> futureTask = new ScheduledFutureTask<>(task);
        delayedExecute(futureTask);
        return futureTask;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Preconditions.checkNotNull(task, "task is null");
        ScheduledFutureTask<T> futureTask = new ScheduledFutureTask<>(task, result);
        delayedExecute(futureTask);
        return futureTask;
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(tasks), "tasks is empty");

        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = new ScheduledFutureTask<>(t);
                futures.add(f);
                execute(f);
            }
            for (Future<T> f : futures) {
                if (!f.isDone()) {
                    try {
                        f.get();
                    } catch (CancellationException | ExecutionException ignore) {
                        //ignore
                    }
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (Future<T> future : futures) {
                    future.cancel(true);
                }
            }
        }
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(tasks), "tasks is empty");

        long realTimeout = timeUnit.convert(timeout, unit);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = new ScheduledFutureTask<>(t);
                futures.add(f);
            }

            long deadline = now() + realTimeout;
            for (Future<T> future : futures) {
                execute((Runnable) future);
                //减去调度任务耗时
                realTimeout = deadline - now();
                if (realTimeout <= 0L) {
                    return futures;
                }
            }

            for (Future<T> f : futures) {
                if (!f.isDone()) {
                    if (realTimeout <= 0L) {
                        return futures;
                    }
                    try {
                        f.get(realTimeout, timeUnit);
                    } catch (CancellationException | ExecutionException ignore) {
                    } catch (TimeoutException toe) {
                        return futures;
                    }
                    realTimeout = deadline - now();
                }
            }
            done = true;
            return futures;
        } finally {
            if (!done) {
                for (Future<T> future : futures) {
                    future.cancel(true);
                }
            }
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return invokeAny(tasks, 0, null);
        } catch (TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(tasks), "tasks is empty");

        boolean timed = timeout > 0;
        long realTimeout = Objects.nonNull(unit) ? timeUnit.convert(timeout, unit) : 0;
        int ntasks = tasks.size();
        ArrayList<Future<T>> futures = new ArrayList<>(ntasks);
        ExecutorCompletionService<T> ecs =
                new ExecutorCompletionService<T>(this);

        try {
            ExecutionException ee = null;
            final long deadline = timed ? now() + realTimeout : 0L;
            Iterator<? extends Callable<T>> it = tasks.iterator();

            // Start one task for sure; the rest incrementally
            futures.add(ecs.submit(it.next()));
            --ntasks;
            int active = 1;

            for (; ; ) {
                Future<T> f = ecs.poll();
                if (f == null) {
                    if (ntasks > 0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));
                        ++active;
                    } else if (active == 0) {
                        break;
                    } else if (timed) {
                        f = ecs.poll(realTimeout, timeUnit);
                        if (f == null) {
                            throw new TimeoutException();
                        }
                        realTimeout = deadline - now();
                    } else {
                        f = ecs.take();
                    }
                }
                if (f != null) {
                    --active;
                    try {
                        return f.get();
                    } catch (ExecutionException eex) {
                        ee = eex;
                    } catch (Exception rex) {
                        ee = new ExecutionException(rex);
                    }
                }
            }

            if (ee == null) {
                ee = new ExecutionException(null);
            }
            throw ee;
        } finally {
            for (Future<T> future : futures) {
                future.cancel(true);
            }
        }
    }

    @Override
    public void execute(Runnable command) {
        Preconditions.checkNotNull(command, "task is null");
        submit(command);
    }

    private void delayedExecute(ScheduledFutureTask<?> task) {
        queue.add(task);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 包装task信息, 装饰器
     */
    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
        /**
         * 间隔时间, nanoTime/millis
         * 固定时间间隔模式, > 0
         * 固定延迟时间模式, < 0
         */
        private final long period;
        /** 触发时间, nanoTime/millis */
        private long triggerTime;

        ScheduledFutureTask(Runnable r) {
            this(r, null, 0, 0);
        }

        ScheduledFutureTask(Runnable r, V result) {
            this(r, result, 0, 0);
        }

        ScheduledFutureTask(Runnable r, long delay) {
            this(r, delay, 0);
        }

        ScheduledFutureTask(Runnable r, long delay, long period) {
            this(r, null, delay, period);
        }

        /**
         * @param delay  延迟时间, nanoTime/millis
         * @param period 间隔时间, nanoTime/millis
         */
        ScheduledFutureTask(Runnable r, V result, long delay, long period) {
            super(r, result);
            this.period = period;
            initNextRunTime(delay, period);
        }

        ScheduledFutureTask(Callable<V> c) {
            this(c, 0, 0);
        }

        ScheduledFutureTask(Callable<V> c, long delay) {
            this(c, delay, 0);
        }

        ScheduledFutureTask(Callable<V> c, long delay, long period) {
            super(c);
            this.period = period;
            initNextRunTime(delay, period);
        }

        /**
         * @return 延迟时间
         */
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(triggerTime - interval(), timeUnit);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public int compareTo(@Nonnull Delayed other) {
            if (other == this) {
                // compare zero if same object
                return 0;
            }
            long diff = triggerTime - ((ScheduledFutureTask) other).triggerTime;
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        /**
         * @return 是否是循环定时任务
         */
        @Override
        public boolean isPeriodic() {
            return period != 0;
        }

        /**
         * 初始化下次触发时间
         */
        private void initNextRunTime(long delay, long period) {
            this.triggerTime = interval() + delay;
            if (period > 0) {
                this.triggerTime += period;
            } else if (period < 0) {
                this.triggerTime += (-period);
            }
            if (triggerTime < 0) {
                triggerTime = Long.MAX_VALUE;
            }
        }

        /**
         * 循环定时任务, 更新下次触发时间
         */
        private void updateNextRunTime() {
            if (period > 0) {
                triggerTime = fixedRate();
            } else {
                triggerTime = fixedDelay();
            }
            if (triggerTime < 0) {
                triggerTime = Long.MAX_VALUE;
            }
        }

        /**
         * 固定时间间隔模式下的触发时间计算
         */
        private long fixedRate() {
            return triggerTime + period;
        }

        /**
         * 固定延迟时间模式下的触发时间计算
         */
        private long fixedDelay() {
            long delay = -period;
            return interval() +
                    ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
        }

        /**
         * 防溢出
         */
        private long overflowFree(long delay) {
            Delayed head = queue.peek();
            if (head != null) {
                long headDelay = head.getDelay(timeUnit);
                if (headDelay < 0 && (delay - headDelay < 0)) {
                    delay = Long.MAX_VALUE + headDelay;
                }
            }
            return delay;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (cancelled) {
                queue.remove(this);
            }
            return cancelled;
        }

        @Override
        public void run() {
            boolean periodic = isPeriodic();
            if (isShutdown() && periodic) {
                //已shutdown, cancel 固定时间间隔的task
                cancel(false);
            } else if (!periodic) {
                //非循环定时任务
                super.run();
            } else if (super.runAndReset()) {
                //循环定时任务
                updateNextRunTime();
                if (!isCancelled()) {
                    //线程还运行中, 入队
                    delayedExecute(this);
                }
            }
        }
    }
}
