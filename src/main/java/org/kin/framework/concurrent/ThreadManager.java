package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by huangjianqin on 2018/1/24.
 */
public class ThreadManager implements ScheduledExecutorService {
    //执行线程
    private ExecutorService executor;
    //调度线程
    private ScheduledExecutorService scheduleExecutor;
    private volatile boolean isStopped;

    public ThreadManager(ExecutorService executor) {
        this(executor, 0);
    }

    public ThreadManager(ExecutorService executor, int scheduleCoreNum) {
        this(executor, scheduleCoreNum, new SimpleThreadFactory("default-schedule-thread-manager"));
    }

    public ThreadManager(ExecutorService executor, int scheduleCoreNum, ThreadFactory scheduleThreadFactory) {
        this.executor = executor;
        if(scheduleCoreNum > 0){
            this.scheduleExecutor = new ScheduledThreadPoolExecutor(scheduleCoreNum, scheduleThreadFactory);
        }
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
        executor.shutdown();
        if(scheduleExecutor != null){
            scheduleExecutor.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        isStopped = true;
        List<Runnable> tasks = Lists.newArrayList();
        tasks.addAll(executor.shutdownNow());
        if(scheduleExecutor != null){
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
        boolean result = executor.awaitTermination(timeout, unit);
        if(scheduleExecutor != null){
            result &= scheduleExecutor.awaitTermination(timeout, unit);
        }
        return result;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return executor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Preconditions.checkArgument(isStopped, "threads is stopped");
        return executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        Preconditions.checkArgument(!isStopped, "threads is stopped");
        executor.execute(command);
    }
}
