package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.kin.framework.utils.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2018/1/24
 */
public class ExecutionContext implements ScheduledExecutorService {
    /** 执行线程 */
    private ExecutorService worker;
    /** 调度线程 */
    private ScheduledExecutorService scheduleExecutor;
    private volatile boolean isStopped;

    public ExecutionContext(ExecutorService worker) {
        this(worker, 0);
    }

    public ExecutionContext(ExecutorService worker, int scheduleParallelism) {
        this(worker, scheduleParallelism, new SimpleThreadFactory("default-schedule-thread-manager"));
    }

    public ExecutionContext(ExecutorService worker, int scheduleParallelism, String scheduleThreadNamePrefix) {
        this(worker, scheduleParallelism, new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public ExecutionContext(ExecutorService worker, int scheduleParallelism, ThreadFactory scheduleThreadFactory) {
        this.worker = worker;
        if (scheduleParallelism > 0) {
            ScheduledThreadPoolExecutor scheduledExecutor = ThreadPoolUtils.scheduledThreadPoolBuilder().coreThreads(scheduleParallelism).threadFactory(scheduleThreadFactory).build();
            //默认future cancel时移除task queue, 稍微加大cpu消耗以及阻塞, 以减少堆内存消耗
            scheduledExecutor.setRemoveOnCancelPolicy(true);
            this.scheduleExecutor = scheduledExecutor;
        }
    }

    //--------------------------------------------------------------------------------------------
    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, 0, null);
    }

    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, scheduleThreadNamePrefix);
    }

    public static ExecutionContext forkjoin(int parallelism, String workerNamePrefix, Thread.UncaughtExceptionHandler handler,
                                            int scheduleParallelism, String scheduleThreadNamePrefix) {
        return forkjoin(new ForkJoinPool(parallelism, new SimpleForkJoinWorkerThradFactory(workerNamePrefix), handler, false),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix) {
        return asyncForkjoin(parallelism, workerNamePrefix, 0, null);
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return asyncForkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, scheduleThreadNamePrefix);
    }

    public static ExecutionContext asyncForkjoin(int parallelism, String workerNamePrefix, Thread.UncaughtExceptionHandler handler,
                                                 int scheduleParallelism, String scheduleThreadNamePrefix) {
        return forkjoin(new ForkJoinPool(parallelism, new SimpleForkJoinWorkerThradFactory(workerNamePrefix), handler, true),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    private static ExecutionContext forkjoin(ForkJoinPool forkJoinPool, int scheduleParallelism, ThreadFactory scheduleThreadFactory) {
        return new ExecutionContext(forkJoinPool, scheduleParallelism, scheduleThreadFactory);
    }

    public static ExecutionContext cache(String workerNamePrefix) {
        return cache(Integer.MAX_VALUE, workerNamePrefix, 0, null);
    }

    public static ExecutionContext cache(ThreadFactory workerThreadFactory) {
        return cache(0, Integer.MAX_VALUE, workerThreadFactory, 0, null);
    }

    public static ExecutionContext cache(String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return cache(0, Integer.MAX_VALUE, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ExecutionContext cache(int maxParallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return cache(0, maxParallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ExecutionContext cache(int coreParallelism, int maxParallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return cache(coreParallelism, maxParallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ExecutionContext cache(int coreParallelism, int maxParallelism, ThreadFactory workerThreadFactory, int scheduleParallelism, ThreadFactory scheduleThreadFactory) {
        return new ExecutionContext(
                ThreadPoolUtils.threadPoolBuilder()
                        .coreThreads(coreParallelism)
                        .maximumThreads(maxParallelism)
                        .keepAlive(60L, TimeUnit.SECONDS)
                        .workQueue(new SynchronousQueue<>())
                        .threadFactory(workerThreadFactory)
                        .build(),
                scheduleParallelism, scheduleThreadFactory);
    }

    public static ExecutionContext fix(int parallelism, String workerNamePrefix) {
        return fix(parallelism, workerNamePrefix, 0, null);
    }

    public static ExecutionContext fix(int parallelism, ThreadFactory workerThreadFactory) {
        return fix(parallelism, workerThreadFactory, 0, null);
    }

    public static ExecutionContext fix(int parallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return fix(parallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ExecutionContext fix(int parallelism, ThreadFactory workerThreadFactory, int scheduleParallelism, ThreadFactory scheduleThreadFactory) {
        return new ExecutionContext(
                ThreadPoolUtils.threadPoolBuilder()
                        .coreThreads(parallelism)
                        .maximumThreads(parallelism)
                        .keepAlive(60L, TimeUnit.SECONDS)
                        .workQueue(new LinkedBlockingQueue<>())
                        .threadFactory(workerThreadFactory)
                        .build(),
                scheduleParallelism, scheduleThreadFactory);
    }

    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, String workerNamePrefix) {
        return elastic(coreParallelism, maxParallelism, new SimpleThreadFactory(workerNamePrefix), 0, null);
    }

    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return elastic(coreParallelism, maxParallelism, new SimpleThreadFactory(workerNamePrefix),
                scheduleParallelism, StringUtils.isBlank(scheduleThreadNamePrefix) ? null : new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    /**
     * 有界扩容的线程池, 允许线程数扩容到一定程度(比如, 10倍CPU核心数), 如果超过这个能力, 则buffer
     */
    public static ExecutionContext elastic(int coreParallelism, int maxParallelism, ThreadFactory workerThreadFactory, int scheduleParallelism, ThreadFactory scheduleThreadFactory) {
        return new ExecutionContext(
                ThreadPoolUtils.eagerThreadPoolBuilder()
                        .coreThreads(coreParallelism)
                        .maximumThreads(maxParallelism)
                        .keepAlive(60L, TimeUnit.SECONDS)
                        .threadFactory(workerThreadFactory)
                        .build(),
                scheduleParallelism, scheduleThreadFactory);
    }
    //--------------------------------------------------------------------------------------------

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        if (!isStopped) {
            return scheduleExecutor.schedule(() -> execute(command), delay, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        if (!isStopped) {
            return scheduleExecutor.schedule(() -> submit(callable).get(), delay, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        if (!isStopped) {
            return scheduleExecutor.scheduleAtFixedRate(() -> execute(command), initialDelay, period, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        if (!isStopped) {
            return scheduleExecutor.scheduleWithFixedDelay(() -> execute(command), initialDelay, delay, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public void shutdown() {
        isStopped = true;
        worker.shutdown();
        if (scheduleExecutor != null) {
            scheduleExecutor.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isStopped = true;
        List<Runnable> tasks = Lists.newArrayList();
        tasks.addAll(worker.shutdownNow());
        if (scheduleExecutor != null) {
            tasks.addAll(scheduleExecutor.shutdownNow());
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
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        boolean result = worker.awaitTermination(timeout, unit);
        if (scheduleExecutor != null) {
            result &= scheduleExecutor.awaitTermination(timeout, unit);
        }
        return result;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        if (!isStopped) {
            return worker.submit(task);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        if (!isStopped) {
            return worker.submit(task, result);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (!isStopped) {
            return worker.submit(task);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (!isStopped) {
            return worker.invokeAll(tasks);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        if (!isStopped) {
            return worker.invokeAll(tasks, timeout, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        if (!isStopped) {
            return worker.invokeAny(tasks);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!isStopped) {
            return worker.invokeAny(tasks, timeout, unit);
        }
        throw new IllegalStateException("threads is stopped");
    }

    @Override
    public void execute(Runnable command) {
        if (!isStopped) {
            worker.execute(command);
        }
    }

    public boolean withSchedule() {
        return Objects.nonNull(scheduleExecutor) && !scheduleExecutor.isShutdown();
    }
}
