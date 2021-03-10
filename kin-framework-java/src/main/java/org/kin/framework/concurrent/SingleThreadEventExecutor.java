package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * 拥有调度能力的单线程Executor
 * 所有消息, 包括调度都在同一Executor(线程)处理
 *
 * @author huangjianqin
 * @date 2020/11/23
 */
public class SingleThreadEventExecutor implements EventExecutor, LoggerOprs {
    //状态枚举
    private static final byte ST_NOT_STARTED = 1;
    private static final byte ST_STARTED = 2;
    private static final byte ST_SHUTTING_DOWN = 3;
    private static final byte ST_SHUTDOWN = 4;
    private static final byte ST_TERMINATED = 5;

    /** 原子更新状态值 */
    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");
    /** 执行线程 */
    private volatile Thread thread;
    /** 线程锁, 用于关闭时阻塞 */
    private final CountDownLatch threadLock = new CountDownLatch(1);

    private final RejectedExecutionHandler rejectedExecutionHandler;
    /** 状态值 */
    private volatile int state = ST_NOT_STARTED;
    /** 任务队列 */
    private final BlockingQueue<ScheduledFutureTask<?>> taskQueue = new DelayQueue<>();
    /** 所属线程池 */
    private final Executor executor;
    /** 绑定线程是否已interrupted */
    private volatile boolean interrupted;
    /**
     * 是否时间敏感(也就是随系统时间发生变化而变化), 则TimeUnit.MILLISECONDS, 否则是TimeUnit.NANOSECONDS
     */
    private final TimeUnit timeUnit;
    /** 所属group */
    private final EventExecutorGroup parent;

    //------------------------------------------------------------------------------------------------------------------------
    private static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }

    //------------------------------------------------------------------------------------------------------------------------
    public SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor) {
        this(parent, executor, false, RejectedExecutionHandler.EMPTY);
    }

    public SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean timeSensitive) {
        this(parent, executor, timeSensitive, RejectedExecutionHandler.EMPTY);
    }

    /**
     * @param timeSensitive 是否时间敏感(也就是随系统时间发生变化而变化)
     */
    public SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean timeSensitive, RejectedExecutionHandler rejectedExecutionHandler) {
        this.parent = parent;
        this.executor = executor;
        if (timeSensitive) {
            timeUnit = TimeUnit.MILLISECONDS;
        } else {
            timeUnit = NANOSECONDS;
        }
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    //------------------------------------------------------------------------------------------------------------------------

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

    @Override
    public void shutdown() {
        synchronized (this) {
            if (state < ST_STARTED || state >= ST_SHUTTING_DOWN) {
                //未开始
                //已结束
                return;
            }

            if (Objects.nonNull(thread)) {
                thread.interrupt();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> taskList = new ArrayList<>();
        synchronized (this) {
            shutdown();

            taskQueue.drainTo(taskList);
            if (!taskQueue.isEmpty()) {
                taskList.addAll(Arrays.asList(taskQueue.toArray(new Runnable[0])));
            }
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
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        Preconditions.checkNotNull(task, "task is null");
        ScheduledFutureTask<T> futureTask = new ScheduledFutureTask<>(task);
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        Preconditions.checkNotNull(task, "task is null");
        ScheduledFutureTask<T> futureTask = new ScheduledFutureTask<>(task, result);
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
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
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
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
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return invokeAny(tasks, 0, null);
        } catch (TimeoutException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(tasks), "tasks is empty");

        boolean timed = timeout > 0;
        long realTimeout = timeUnit.convert(timeout, unit);
        int ntasks = tasks.size();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
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
    public void execute(@Nonnull Runnable command) {
        Preconditions.checkNotNull(command, "task is null");

        lazyExecute(new ScheduledFutureTask<>(command));
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> futureTask = new ScheduledFutureTask<>(command, timeUnit.convert(delay, unit));
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> futureTask = new ScheduledFutureTask<>(command, timeUnit.convert(initialDelay, unit), timeUnit.convert(period, unit));
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(callable, "task is null");

        ScheduledFutureTask<V> futureTask = new ScheduledFutureTask<>(callable, timeUnit.convert(delay, unit));
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> futureTask = new ScheduledFutureTask<>(command, timeUnit.convert(initialDelay, unit), timeUnit.convert(-delay, unit));
        lazyExecute(futureTask);
        return futureTask;
    }

    //------------------------------------------------------------------------------------------------------------------------

    /**
     * 任务入队
     */
    private void addTask(ScheduledFutureTask<?> task) {
        if (state > ST_STARTED) {
            return;
        }
        taskQueue.offer(task);
    }

    /**
     * 懒初始化, 并执行task
     */
    private void lazyExecute(ScheduledFutureTask<?> task) {
        addTask(task);
        if (!isInEventLoop()) {
            if (state > ST_NOT_STARTED) {
                return;
            }
            synchronized (this) {
                if (state > ST_NOT_STARTED) {
                    return;
                }
                startThread();
                if (isShutdown()) {
                    boolean reject = false;
                    try {
                        if (removeTask(task)) {
                            reject = true;
                        }
                    } catch (UnsupportedOperationException e) {
                        //do nothing
                    }
                    if (reject) {
                        reject();
                    }
                }
            }
        }
    }

    /**
     * 启动线程
     */
    private void startThread() {
        if (state == ST_NOT_STARTED) {
            if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                boolean success = false;
                try {
                    executor.execute(new InternalLoop());
                    success = true;
                } finally {
                    if (!success) {
                        STATE_UPDATER.compareAndSet(this, ST_STARTED, ST_NOT_STARTED);
                    }
                }
            }
        }
    }

    /**
     * 移除task
     */
    private boolean removeTask(ScheduledFutureTask<?> task) {
        return taskQueue.remove(task);

    }

    /**
     * 拒绝执行task
     */
    private void reject(Runnable task) {
        rejectedExecutionHandler.rejected(task, this);
    }

    /**
     * Interrupt the current running {@link Thread}.
     */
    public void interrupt() {
        Thread currentThread = thread;
        if (currentThread == null) {
            interrupted = true;
        } else {
            currentThread.interrupt();
        }
    }

    /**
     * 取出task
     */
    private ScheduledFutureTask<?> takeTask() throws InterruptedException {
        return taskQueue.take();
    }

    /**
     * 取消所有未执行的task
     */
    private void cancelAllTasks() {
        ScheduledFutureTask<?>[] scheduledFutureTasks = taskQueue.toArray(new ScheduledFutureTask<?>[0]);
        for (ScheduledFutureTask<?> futureTask : scheduledFutureTasks) {
            futureTask.cancel(true);
        }
    }

    /**
     * 是否运行中
     */
    private boolean isRunning() {
        return state == ST_STARTED;
    }

    @Override
    public EventExecutorGroup parent() {
        return parent;
    }

    @Override
    public boolean isInEventLoop(Thread thread) {
        if (Objects.nonNull(this.thread)) {
            return this.thread == thread;
        }

        return false;
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     * 包装task信息, 装饰器
     */
    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {
        /**
         * 间隔时间, nanoTime
         * 固定时间间隔模式, > 0
         * 固定延迟时间模式, < 0
         */
        private final long period;
        /** 触发时间, nanoTime */
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
         * @param delay  延迟时间, nanoTime
         * @param period 间隔时间, nanoTime
         */
        ScheduledFutureTask(Runnable r, V result, long delay, long period) {
            super(r, result);
            this.period = period;
            this.triggerTime = now() + delay + period;
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
            this.triggerTime = now() + delay + period;
        }

        /**
         * @return 延迟时间
         */
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(triggerTime - now(), timeUnit);
        }

        @Override
        public int compareTo(@Nonnull Delayed other) {
            if (other == this) // compare zero if same object
            {
                return 0;
            }
            long diff = getDelay(timeUnit) - other.getDelay(timeUnit);
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
         * 循环定时任务, 更新下次触发时间
         */
        private void setNextRunTime() {
            if (period > 0) {
                triggerTime = fixedRate();
            } else {
                triggerTime = fixedDelay();
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
            return now() +
                    ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
        }

        /**
         * 防溢出
         */
        private long overflowFree(long delay) {
            Delayed head = SingleThreadEventExecutor.this.taskQueue.peek();
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
                removeTask(this);
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
                setNextRunTime();
                if (isRunning()) {
                    //线程还运行中, 入队
                    addTask(this);
                }
            }
        }
    }

    /**
     * 线程run方法逻辑
     */
    private class InternalLoop implements Runnable {
        @Override
        public void run() {
            thread = Thread.currentThread();
            if (interrupted) {
                thread.interrupt();
            }

            try {
                for (; ; ) {
                    try {
                        ScheduledFutureTask<?> task = takeTask();
                        task.run();
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            return;
                        }
                        error("Unexpected exception from an runned Task: ", e);
                    }
                }
            } catch (Throwable t) {
                //do nothing
            } finally {
                for (; ; ) {
                    int oldState = state;
                    if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER.compareAndSet(
                            SingleThreadEventExecutor.this, oldState, ST_SHUTTING_DOWN)) {
                        break;
                    }
                }

                try {
                    cancelAllTasks();
                    for (; ; ) {
                        int oldState = state;
                        if (oldState >= ST_SHUTDOWN || STATE_UPDATER.compareAndSet(
                                SingleThreadEventExecutor.this, oldState, ST_SHUTDOWN)) {
                            break;
                        }
                    }
                    cancelAllTasks();

                    //清理资源
                } finally {
                    STATE_UPDATER.set(SingleThreadEventExecutor.this, ST_TERMINATED);
                    threadLock.countDown();
                }
            }
        }
    }
}
