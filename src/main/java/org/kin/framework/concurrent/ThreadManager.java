package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2018/1/24
 */
public class ThreadManager implements ScheduledExecutorService {
    /** 执行线程 */
    private ExecutorService worker;
    /** 调度线程 */
    private ScheduledExecutorService scheduleExecutor;
    private volatile boolean isStopped;

    public ThreadManager(ExecutorService worker) {
        this(worker, 0);
    }

    public ThreadManager(ExecutorService worker, int scheduleParallelism) {
        this(worker, scheduleParallelism, new SimpleThreadFactory("default-schedule-thread-manager"));
    }

    public ThreadManager(ExecutorService worker, int scheduleParallelism, ThreadFactory scheduleThreadFactory) {
        this.worker = worker;
        if (scheduleParallelism > 0) {
            this.scheduleExecutor = new ScheduledThreadPoolExecutor(scheduleParallelism, scheduleThreadFactory);
        }
    }

    //--------------------------------------------------------------------------------------------
    public static ThreadManager forkjoin(int parallelism, String workerNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, 0, "");
    }

    public static ThreadManager forkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, scheduleThreadNamePrefix);
    }

    public static ThreadManager asyncForkjoin(int parallelism, String workerNamePrefix) {
        return asyncForkjoin(parallelism, workerNamePrefix, 0, "");
    }

    public static ThreadManager asyncForkjoin(int parallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return asyncForkjoin(parallelism, workerNamePrefix, null, scheduleParallelism, scheduleThreadNamePrefix);
    }

    public static ThreadManager forkjoin(int parallelism, String workerNamePrefix, Thread.UncaughtExceptionHandler handler,
                                         int scheduleParallelism, String scheduleThreadNamePrefix) {
        return forkjoin(new ForkJoinPool(parallelism, new SimpleForkJoinWorkerThradFactory(workerNamePrefix), handler, false),
                scheduleParallelism, scheduleThreadNamePrefix);
    }

    public static ThreadManager asyncForkjoin(int parallelism, String workerNamePrefix, Thread.UncaughtExceptionHandler handler,
                                              int scheduleParallelism, String scheduleThreadNamePrefix) {
        return forkjoin(new ForkJoinPool(parallelism, new SimpleForkJoinWorkerThradFactory(workerNamePrefix), handler, true),
                scheduleParallelism, scheduleThreadNamePrefix);
    }

    private static ThreadManager forkjoin(ForkJoinPool forkJoinPool, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return new ThreadManager(forkJoinPool, scheduleParallelism, new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ThreadManager cache(String workerNamePrefix) {
        return forkjoin(Integer.MAX_VALUE, workerNamePrefix, 0, "");
    }

    public static ThreadManager cache(int maxParallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return new ThreadManager(
                new ThreadPoolExecutor(0, maxParallelism, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new SimpleThreadFactory(workerNamePrefix)),
                scheduleParallelism, new SimpleThreadFactory(scheduleThreadNamePrefix));
    }

    public static ThreadManager fix(int parallelism, String workerNamePrefix) {
        return forkjoin(parallelism, workerNamePrefix, 0, "");
    }

    public static ThreadManager fix(int parallelism, String workerNamePrefix, int scheduleParallelism, String scheduleThreadNamePrefix) {
        return new ThreadManager(
                new ThreadPoolExecutor(parallelism, parallelism, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new SimpleThreadFactory(workerNamePrefix)),
                scheduleParallelism, new SimpleThreadFactory(scheduleThreadNamePrefix));
    }
    //--------------------------------------------------------------------------------------------

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return scheduleExecutor.schedule(() -> execute(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return scheduleExecutor.schedule(() -> submit(callable).get(), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return scheduleExecutor.scheduleAtFixedRate(() -> execute(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Preconditions.checkNotNull(scheduleExecutor);
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return scheduleExecutor.scheduleWithFixedDelay(() -> execute(command), initialDelay, delay, unit);
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
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return worker.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return worker.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return worker.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return worker.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Preconditions.checkArgument(isStopped, "threads is stopped");
        return worker.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return worker.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return worker.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        worker.execute(command);
    }
}
