package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.collection.DefaultPriorityQueue;
import org.kin.framework.collection.PriorityQueue;
import org.kin.framework.collection.PriorityQueueNode;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * 拥有调度能力的单线程Executor
 * 所有消息逻辑(包括消息调度)都在同一线程处理
 * 不建议每个消息处理消耗过长时间(比如, IO操作)
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2020/11/23
 */
public class SingleThreadEventExecutor implements EventExecutor, LoggerOprs {
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
    private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");
    /** 调度task 通用Comparator */
    private static final Comparator<ScheduledFutureTask<?>>
            SCHEDULED_FUTURE_TASK_COMPARATOR = ScheduledFutureTask::compareTo;

    /** 实例创建时间 */
    private final long createTime = now();

    /** 执行线程 */
    private volatile Thread thread;
    /** 线程锁, 用于关闭时阻塞 */
    private final CountDownLatch terminationLatch = new CountDownLatch(1);

    /** task reject逻辑实现 */
    private final RejectedExecutionHandler rejectedExecutionHandler;
    /** 状态值 */
    private volatile int state = ST_NOT_STARTED;
    /** 任务队列 */
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    /** 调度任务队列 */
    private final PriorityQueue<ScheduledFutureTask<?>> scheduledTaskQueue = new DefaultPriorityQueue<>(SCHEDULED_FUTURE_TASK_COMPARATOR, 11);
    /** 所属线程池 */
    private final Executor executor;
    /** 绑定线程是否已interrupted */
    private volatile boolean interrupted;
    /** 所属group */
    private final EventExecutorGroup parent;

    //------------------------------------------------------------------------------------------------------------------------
    private static void reject() {
        throw new RejectedExecutionException("event executor terminated");
    }

    //------------------------------------------------------------------------------------------------------------------------
    public SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor) {
        this(parent, executor, RejectedExecutionHandler.EMPTY);
    }

    public SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, RejectedExecutionHandler rejectedExecutionHandler) {
        this.parent = parent;
        this.executor = executor;
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }

    //------------------------------------------------------------------------------------------------------------------------

    /**
     * 获取当前时间
     */
    private long now() {
        return System.nanoTime();
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
    public void shutdown() {
        synchronized (this) {
            if (state >= ST_SHUTTING_DOWN) {
                //已结束
                return;
            }
            if (state < ST_STARTED) {
                //未开始
                STATE_UPDATER.set(SingleThreadEventExecutor.this, ST_TERMINATED);
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
            taskList.addAll(Arrays.asList(scheduledTaskQueue.toArray(new Runnable[0])));
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
        synchronized (this) {
            for (; ; ) {
                if (state >= ST_TERMINATED) {
                    return true;
                }
                if (timeout <= 0) {
                    return false;
                }

                terminationLatch.await(timeout, unit);
            }
        }
    }

    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        Preconditions.checkNotNull(task, "task is null");
        ScheduledFutureTask<T> futureTask = new ScheduledFutureTask<>(task);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        Preconditions.checkNotNull(task, "task is null");
        ScheduledFutureTask<T> futureTask = new ScheduledFutureTask<>(task, result);
        execute(futureTask);
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

        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<>(tasks.size());
        boolean done = false;
        try {
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = new ScheduledFutureTask<>(t);
                futures.add(f);
            }

            long deadline = now() + nanos;
            for (Future<T> future : futures) {
                execute((Runnable) future);
                //减去调度任务耗时
                nanos = deadline - now();
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
                    nanos = deadline - now();
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
        long nanos = Objects.nonNull(unit) ? unit.toNanos(timeout) : 0;
        int ntasks = tasks.size();
        ArrayList<Future<T>> futures = new ArrayList<>(ntasks);
        ExecutorCompletionService<T> ecs =
                new ExecutorCompletionService<T>(this);

        try {
            ExecutionException ee = null;
            final long deadline = timed ? now() + nanos : 0L;
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
                        nanos = deadline - now();
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

        execute0(command);
    }

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        return schedule(new ScheduledFutureTask<>(command, unit.toNanos(delay)));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        return schedule(new ScheduledFutureTask<>(command, unit.toNanos(initialDelay), unit.toNanos(period)));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(callable, "task is null");

        return schedule(new ScheduledFutureTask<>(callable, unit.toNanos(delay)));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(command, "task is null");

        return schedule(new ScheduledFutureTask<>(command, unit.toNanos(initialDelay), -unit.toNanos(delay)));
    }

    //------------------------------------------------------------------------------------------------------------------------

    /**
     * task入队
     */
    private void addTask(Runnable task) {
        if (state > ST_STARTED) {
            reject();
        }
        if (!taskQueue.offer(task)) {
            reject(task);
        }
    }

    /**
     * 执行task同一入口
     */
    private void execute0(Runnable task) {
        addTask(task);
        boolean inEventLoop = isInEventLoop();
        if (!inEventLoop) {
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
                    executor.execute(new Loop());
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
    private boolean removeTask(Runnable task) {
        return taskQueue.remove(task);
    }

    /**
     * 调度task统一接口
     */
    private <V> ScheduledFuture<V> schedule(ScheduledFutureTask<V> task) {
        if (isInEventLoop()) {
            scheduledTaskQueue.add(task);
        } else {
            //not in event loop, just execute and in event loop
            execute(() -> schedule(task));
        }

        return task;
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
     * @return 调度队列头的调度task
     */
    private ScheduledFutureTask<?> peekScheduledTask() {
        return scheduledTaskQueue.peek();
    }

    /**
     * 从调度队列fetch并把task push到taskqueue
     */
    private void fetchFromScheduledTaskQueue() {
        if (scheduledTaskQueue.isEmpty()) {
            return;
        }

        long deadlineTime = now() - createTime;
        for (; ; ) {
            Runnable scheduledTask = pollScheduledTask(deadlineTime);
            if (scheduledTask == null) {
                return;
            }
            if (!taskQueue.offer(scheduledTask)) {
                // No space left in the task queue add it back to the scheduledTaskQueue so we pick it up again.
                scheduledTaskQueue.add((ScheduledFutureTask<?>) scheduledTask);
                return;
            }
        }
    }

    /**
     * 从队列头开始移除过期的调度task
     */
    private Runnable pollScheduledTask(long deadlineTime) {
        ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
        if (scheduledTask == null || scheduledTask.triggerTime - deadlineTime > 0) {
            return null;
        }
        return scheduledTaskQueue.remove();
    }

    /**
     * main
     * 取出task
     */
    private Runnable takeTask() throws InterruptedException {
        for (; ; ) {
            ScheduledFutureTask<?> scheduledTask = peekScheduledTask();
            if (scheduledTask == null) {
                return taskQueue.take();
            } else {
                long delayTime = scheduledTask.getDelay(TimeUnit.NANOSECONDS);
                Runnable task = null;
                if (delayTime > 0) {
                    task = taskQueue.poll(delayTime, TimeUnit.NANOSECONDS);
                }

                if (task == null) {
                    // We need to fetch the scheduled tasks now as otherwise there may be a chance that
                    // scheduled tasks are never executed if there is always one task in the taskQueue.
                    fetchFromScheduledTaskQueue();
                    task = taskQueue.poll();
                }

                if (task != null) {
                    return task;
                }
            }
        }
    }

    /**
     * 取消所有未执行的task
     */
    private void cancelAllTasks() {
        cancelScheduledTasks();
    }

    /**
     * 取消所有未执行的调度task
     */
    private void cancelScheduledTasks() {
        if (CollectionUtils.isNonEmpty(scheduledTaskQueue)) {
            return;
        }

        ScheduledFutureTask<?>[] scheduledTasks =
                scheduledTaskQueue.toArray(new ScheduledFutureTask<?>[0]);

        for (ScheduledFutureTask<?> task : scheduledTasks) {
            task.cancel(false);
        }

        scheduledTaskQueue.clear();
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
    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V>, PriorityQueueNode {
        /**
         * 间隔时间, nanoTime
         * 固定时间间隔模式, > 0
         * 固定延迟时间模式, < 0
         */
        private final long period;
        /** 触发时间, nanoTime */
        private long triggerTime;
        /** priority queue下标, 用于排序 */
        private int queueIndex = INDEX_NOT_IN_QUEUE;

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
            return unit.convert(triggerTime - interval(), unit);
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
            Delayed head = peekScheduledTask();
            if (head != null) {
                long headDelay = head.getDelay(TimeUnit.NANOSECONDS);
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
                updateNextRunTime();
                if (!isCancelled()) {
                    //线程还运行中, 入队
                    schedule(this);
                }
            }
        }

        @Override
        public int priorityQueueIndex(DefaultPriorityQueue<?> queue) {
            return queueIndex;
        }

        @Override
        public void priorityQueueIndex(DefaultPriorityQueue<?> queue, int i) {
            queueIndex = i;
        }
    }

    /**
     * 线程run方法逻辑
     */
    private class Loop implements Runnable {
        @Override
        public void run() {
            thread = Thread.currentThread();
            if (interrupted) {
                thread.interrupt();
            }

            try {
                for (; ; ) {
                    try {
                        Runnable task = takeTask();
                        task.run();
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            break;
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
                    terminationLatch.countDown();
                }
            }
        }
    }
}
