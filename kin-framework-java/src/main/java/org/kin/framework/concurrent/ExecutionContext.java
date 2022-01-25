package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.kin.framework.utils.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2018/1/24
 */
public class ExecutionContext implements ScheduledExecutorService {
    /** 默认scheduler后缀, scheduler name = worker name + {@link #DEFAULT_SCHEDULER_NAME} */
    public static final String DEFAULT_SCHEDULER_NAME = "-scheduler";
    /** 执行线程 */
    private final ExecutorService worker;
    /** 调度线程 */
    private ScheduledExecutorService scheduler;
    private volatile boolean isStopped;

    public ExecutionContext(ExecutorService worker) {
        this(worker, 0);
    }

    public ExecutionContext(ExecutorService worker, int scheduleParallelism) {
        this(worker, scheduleParallelism, new SimpleThreadFactory("default".concat(DEFAULT_SCHEDULER_NAME)));
    }

    public ExecutionContext(ExecutorService worker, int scheduleParallelism, String schedulerPrefix) {
        this(worker, scheduleParallelism, new SimpleThreadFactory(schedulerPrefix));
    }

    public ExecutionContext(ExecutorService worker, int scheduleParallelism, ThreadFactory schedulerFactory) {
        this.worker = worker;
        if (scheduleParallelism > 0) {
            ScheduledThreadPoolExecutor scheduledExecutor = ThreadPoolUtils.scheduledThreadPoolBuilder().coreThreads(scheduleParallelism).threadFactory(schedulerFactory).build();
            //默认future cancel时移除task queue, 稍微加大cpu消耗以及阻塞, 以减少堆内存消耗
            scheduledExecutor.setRemoveOnCancelPolicy(true);
            this.scheduler = scheduledExecutor;
        }
    }

    //--------------------------------------------------------------------------------------------
    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, 0, null);
    }

    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism) {
        return forkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, workerNamePrefix.concat(DEFAULT_SCHEDULER_NAME));
    }

    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, schedulerNamePrefix);
    }

    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix, Thread.UncaughtExceptionHandler handler,
                                            int scheduleParallelism, String schedulerNamePrefix) {
        return forkjoin(ThreadPoolUtils.forkJoinThreadPoolBuilder()
                        .poolName(workerNamePrefix)
                        .parallelism(parallelism)
                        .threadFactory(new SimpleForkJoinWorkerThreadFactory(workerNamePrefix))
                        .uncaughtExceptionHandler(handler)
                        .build(),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    private static ExecutionContext forkjoin(ForkJoinPool forkJoinPool, int scheduleParallelism, ThreadFactory schedulerFactory) {
        return new ExecutionContext(forkJoinPool, scheduleParallelism, schedulerFactory);
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix) {
        return asyncForkjoin(parallelism, workerNamePrefix, 0, null);
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism) {
        return asyncForkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, workerNamePrefix.concat(DEFAULT_SCHEDULER_NAME));
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return asyncForkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, schedulerNamePrefix);
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix, Thread.UncaughtExceptionHandler handler,
                                                 int scheduleParallelism, String schedulerNamePrefix) {
        return forkjoin(ThreadPoolUtils.forkJoinThreadPoolBuilder()
                        .poolName(workerNamePrefix)
                        .parallelism(parallelism)
                        .threadFactory(new SimpleForkJoinWorkerThreadFactory(workerNamePrefix))
                        .uncaughtExceptionHandler(handler)
                        .async()
                        .build(),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    public static ExecutionContext cache(String workerNamePrefix) {
        return cache(Integer.MAX_VALUE, workerNamePrefix, 0, null);
    }

    public static ExecutionContext cache(ThreadFactory workerThreadFactory) {
        return cache(0, Integer.MAX_VALUE, workerThreadFactory, 0, null);
    }

    public static ExecutionContext cache(String workerNamePrefix, int scheduleParallelism) {
        return cache(workerNamePrefix, scheduleParallelism, workerNamePrefix.concat(DEFAULT_SCHEDULER_NAME));
    }

    public static ExecutionContext cache(String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return cache(0, Integer.MAX_VALUE, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    public static ExecutionContext cache(int maxParallelism, String workerNamePrefix, int scheduleParallelism) {
        return cache(maxParallelism, workerNamePrefix, scheduleParallelism, workerNamePrefix.concat(DEFAULT_SCHEDULER_NAME));
    }

    public static ExecutionContext cache(int maxParallelism, String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return cache(0, maxParallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    public static ExecutionContext cache(int coreParallelism, int maxParallelism, String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return cache(coreParallelism, maxParallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    public static ExecutionContext cache(int coreParallelism, int maxParallelism, ThreadFactory workerThreadFactory, int scheduleParallelism, ThreadFactory schedulerFactory) {
        return new ExecutionContext(
                ThreadPoolUtils.threadPoolBuilder()
                        .coreThreads(coreParallelism)
                        .maximumThreads(maxParallelism)
                        .keepAlive(60L, TimeUnit.SECONDS)
                        .workQueue(new SynchronousQueue<>())
                        .threadFactory(workerThreadFactory)
                        .common(),
                scheduleParallelism, schedulerFactory);
    }

    public static ExecutionContext fix(int parallelism, String workerNamePrefix) {
        return fix(parallelism, workerNamePrefix, 0, null);
    }

    public static ExecutionContext fix(int parallelism, ThreadFactory workerThreadFactory) {
        return fix(parallelism, workerThreadFactory, 0, null);
    }

    public static ExecutionContext fix(int parallelism, String workerNamePrefix, int scheduleParallelism) {
        return fix(parallelism, workerNamePrefix, scheduleParallelism, workerNamePrefix.concat(DEFAULT_SCHEDULER_NAME));
    }

    public static ExecutionContext fix(int parallelism, String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return fix(parallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    public static ExecutionContext fix(int parallelism, ThreadFactory workerThreadFactory, int scheduleParallelism, ThreadFactory schedulerFactory) {
        return new ExecutionContext(
                ThreadPoolUtils.threadPoolBuilder()
                        .coreThreads(parallelism)
                        .maximumThreads(parallelism)
                        .keepAlive(60L, TimeUnit.SECONDS)
                        .workQueue(new LinkedBlockingQueue<>())
                        .threadFactory(workerThreadFactory)
                        .common(),
                scheduleParallelism, schedulerFactory);
    }

    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, String workerNamePrefix) {
        return elastic(coreParallelism, maxParallelism, new SimpleThreadFactory(workerNamePrefix), 0, null);
    }

    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, ThreadFactory workerThreadFactory) {
        return elastic(coreParallelism, maxParallelism, workerThreadFactory, 0, null);
    }

    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, String workerNamePrefix, int scheduleParallelism) {
        return elastic(coreParallelism, maxParallelism, workerNamePrefix, scheduleParallelism, workerNamePrefix.concat(DEFAULT_SCHEDULER_NAME));
    }

    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, String workerNamePrefix, int scheduleParallelism, String schedulerNamePrefix) {
        return elastic(coreParallelism, maxParallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(schedulerNamePrefix) ? null : new SimpleThreadFactory(schedulerNamePrefix));
    }

    /**
     * 有界扩容的线程池, 允许线程数扩容到一定程度(比如, 10倍CPU核心数), 如果超过这个能力, 则buffer
     */
    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, ThreadFactory workerThreadFactory, int scheduleParallelism, ThreadFactory schedulerFactory) {
        return new ExecutionContext(
                ThreadPoolUtils.threadPoolBuilder()
                        .coreThreads(coreParallelism)
                        .maximumThreads(maxParallelism)
                        .keepAlive(60L, TimeUnit.SECONDS)
                        .threadFactory(workerThreadFactory)
                        .eager(),
                scheduleParallelism, schedulerFactory);
    }
    //--------------------------------------------------------------------------------------------

    @Override
    public ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(scheduler);
        if (!isStopped) {
            return scheduler.schedule(() -> execute(command), delay, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(scheduler);
        if (!isStopped) {
            return scheduler.schedule(() -> submit(callable).get(), delay, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(scheduler);
        if (!isStopped) {
            return scheduler.scheduleAtFixedRate(() -> execute(command), initialDelay, period, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        Preconditions.checkNotNull(scheduler);
        if (!isStopped) {
            return scheduler.scheduleWithFixedDelay(() -> execute(command), initialDelay, delay, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public void shutdown() {
        if (isStopped) {
            return;
        }

        isStopped = true;
        worker.shutdown();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if (isStopped) {
            return Collections.emptyList();
        }

        isStopped = true;
        List<Runnable> tasks = Lists.newArrayList();
        tasks.addAll(worker.shutdownNow());
        if (scheduler != null) {
            tasks.addAll(scheduler.shutdownNow());
        }
        return tasks;
    }

    @Override
    public boolean isShutdown() {
        return isStopped;
    }

    @Override
    public boolean isTerminated() {
        return isStopped;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        boolean result = worker.awaitTermination(timeout, unit);
        if (scheduler != null) {
            result &= scheduler.awaitTermination(timeout, unit);
        }
        return result;
    }

    @Override
    public <T> Future<T> submit(@Nonnull Callable<T> task) {
        if (!isStopped) {
            return worker.submit(task);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> Future<T> submit(@Nonnull Runnable task, T result) {
        if (!isStopped) {
            return worker.submit(task, result);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public Future<?> submit(@Nonnull Runnable task) {
        if (!isStopped) {
            return worker.submit(task);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (!isStopped) {
            return worker.invokeAll(tasks);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        if (!isStopped) {
            return worker.invokeAll(tasks, timeout, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (!isStopped) {
            return worker.invokeAny(tasks);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isStopped) {
            return worker.invokeAny(tasks, timeout, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        if (!isStopped) {
            worker.execute(command);
        }
    }

    public boolean withSchedule() {
        return Objects.nonNull(scheduler) && !scheduler.isShutdown();
    }
}
