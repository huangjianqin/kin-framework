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
 * st = single thread
 * 拥有调度能力的单线程Executor
 * 调度完后, task执行仍然在该Executor
 *
 * @author huangjianqin
 * @date 2020/11/23
 */
public class StScheduledExecutor implements ScheduledExecutorService, LoggerOprs {
    //状态枚举
    private static final byte ST_NOT_STARTED = 1;
    private static final byte ST_STARTED = 2;
    private static final byte ST_SHUTTING_DOWN = 3;
    private static final byte ST_SHUTDOWN = 4;
    private static final byte ST_TERMINATED = 5;

    /** 原子更新状态值 */
    private static final AtomicIntegerFieldUpdater<StScheduledExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(StScheduledExecutor.class, "state");
    /** 执行线程 */
    private volatile Thread thread;
    /** 线程锁, 用于关闭时阻塞 */
    private final CountDownLatch threadLock = new CountDownLatch(1);

    private final RejectedExecutionHandler rejectedExecutionHandler;
    /** 状态值 */
    private volatile int state = ST_NOT_STARTED;
    /** 任务队列 */
    private final BlockingQueue<ScheduledFutureTask<?>> taskQueue = new DelayQueue<>();
    /** 所在线程池 */
    private final ExecutorService parent;
    /** 绑定线程是否已interrupted */
    private volatile boolean interrupted;

    //------------------------------------------------------------------------------------------------------------------------
    private static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }

    //------------------------------------------------------------------------------------------------------------------------
    public StScheduledExecutor(ExecutorService parent) {
        this(RejectedExecutionHandler.EMPTY, parent);
    }

    public StScheduledExecutor(RejectedExecutionHandler rejectedExecutionHandler, ExecutorService parent) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.parent = parent;
    }

    //------------------------------------------------------------------------------------------------------------------------
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
                for (Runnable r : taskQueue.toArray(new Runnable[0])) {
                    if (taskQueue.remove(r)) {
                        taskList.add(r);
                    }
                }
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
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
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(tasks), "tasks is empty");

        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = new ScheduledFutureTask<>(t);
                futures.add(f);
            }

            long deadline = System.nanoTime() + nanos;
            for (Future<T> future : futures) {
                execute((Runnable) future);
                //减去调度任务耗时
                nanos = deadline - System.nanoTime();
                if (nanos <= 0L) {
                    return futures;
                }
            }

            for (Future<T> f : futures) {
                if (!f.isDone()) {
                    if (nanos <= 0L) {
                        return futures;
                    }
                    try {
                        f.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (CancellationException | ExecutionException ignore) {
                    } catch (TimeoutException toe) {
                        return futures;
                    }
                    nanos = deadline - System.nanoTime();
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
            //ignore
        }
        return null;
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(tasks), "tasks is empty");

        boolean timed = timeout > 0;
        long nanos = unit.toNanos(timeout);
        int ntasks = tasks.size();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
        ExecutorCompletionService<T> ecs =
                new ExecutorCompletionService<T>(this);

        try {
            ExecutionException ee = null;
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
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
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                        if (f == null) {
                            throw new TimeoutException();
                        }
                        nanos = deadline - System.nanoTime();
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
                    } catch (RuntimeException rex) {
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
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> futureTask = new ScheduledFutureTask<>(command, unit.toNanos(delay));
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(callable, "task is null");

        ScheduledFutureTask<V> futureTask = new ScheduledFutureTask<>(callable, unit.toNanos(delay));
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> futureTask = new ScheduledFutureTask<>(command, unit.toNanos(initialDelay), unit.toNanos(period));
        lazyExecute(futureTask);
        return futureTask;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        ScheduledFutureTask<?> futureTask = new ScheduledFutureTask<>(command, unit.toNanos(initialDelay), unit.toNanos(-delay));
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
        if (!isInThread()) {
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
     * @return 当前线程是否绑定线程
     */
    private boolean isInThread() {
        if (Objects.nonNull(thread)) {
            return Thread.currentThread() == thread;
        }

        return false;
    }

    /**
     * 启动线程
     */
    private void startThread() {
        if (state == ST_NOT_STARTED) {
            if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                boolean success = false;
                try {
                    parent.execute(new Loop());
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
     * 循环执行task
     */
    private void loopRunTask() {
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
            this.triggerTime = System.nanoTime() + delay + period;
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
            this.triggerTime = System.nanoTime() + delay + period;
        }

        /**
         * @return 延迟时间
         */
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(triggerTime - System.nanoTime(), NANOSECONDS);
        }

        @Override
        public int compareTo(@Nonnull Delayed other) {
            if (other == this) // compare zero if same object
            {
                return 0;
            }
            long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
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
            return System.nanoTime() +
                    ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
        }

        /**
         * 防溢出
         */
        private long overflowFree(long delay) {
            Delayed head = StScheduledExecutor.this.taskQueue.peek();
            if (head != null) {
                long headDelay = head.getDelay(NANOSECONDS);
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
     * 线程逻辑
     */
    private class Loop implements Runnable {
        @Override
        public void run() {
            thread = Thread.currentThread();
            if (interrupted) {
                thread.interrupt();
            }

            try {
                loopRunTask();
            } catch (Throwable t) {
                if (!(t instanceof InterruptedException)) {
                    error("Unexpected exception from an scheduled executor: ", t);
                }
            } finally {
                for (; ; ) {
                    int oldState = state;
                    if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER.compareAndSet(
                            StScheduledExecutor.this, oldState, ST_SHUTTING_DOWN)) {
                        break;
                    }
                }

                try {
                    cancelAllTasks();
                    for (; ; ) {
                        int oldState = state;
                        if (oldState >= ST_SHUTDOWN || STATE_UPDATER.compareAndSet(
                                StScheduledExecutor.this, oldState, ST_SHUTDOWN)) {
                            break;
                        }
                    }
                    cancelAllTasks();

                    //清理资源
                } finally {
                    STATE_UPDATER.set(StScheduledExecutor.this, ST_TERMINATED);
                    threadLock.countDown();
                }
            }
        }
    }
}
