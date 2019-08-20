package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.Constants;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by huangjianqin on 2018/1/24.
 */
public class ThreadManager implements ScheduledExecutorService {
    public static ThreadManager DEFAULT;
    public static final ExecutorType DEFAULT_EXECUTOR_TYPE;

    private static int getScheduleCoreNum(){
        return SysUtils.getSuitableThreadNum() / 10 + 1;
    }
    static {
        String executorTypeStr = System.getenv(Constants.DEFAULT_EXECUTOR);
        if (StringUtils.isNotBlank(executorTypeStr)) {
            DEFAULT_EXECUTOR_TYPE = ExecutorType.getByName(executorTypeStr);
            DEFAULT = new ThreadManager(DEFAULT_EXECUTOR_TYPE.getExecutor(), getScheduleCoreNum());
        } else {
            DEFAULT_EXECUTOR_TYPE = ExecutorType.THREADPOOL;
            DEFAULT = new ThreadManager(DEFAULT_EXECUTOR_TYPE.getExecutor(), getScheduleCoreNum());
        }

        JvmCloseCleaner.DEFAULT().add(() -> {
            DEFAULT.shutdown();
        });
    }

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

    public static ThreadManager forkJoinPoolThreadManager() {
        return new ThreadManager(ExecutorType.FORKJOIN.getExecutor());
    }

    public static ThreadManager forkJoinPoolThreadManagerWithScheduled(int scheduleCoreNum) {
        return new ThreadManager(ExecutorType.FORKJOIN.getExecutor(), scheduleCoreNum);
    }

    public static ThreadManager forkJoinPoolThreadManagerWithScheduled(int scheduleCoreNum, ThreadFactory scheduleThreadFactory) {
        return new ThreadManager(ExecutorType.FORKJOIN.getExecutor(), scheduleCoreNum, scheduleThreadFactory);
    }


    public static ThreadManager commonThreadManager() {
        return new ThreadManager(ExecutorType.THREADPOOL.getExecutor());
    }

    public static ThreadManager commonThreadManagerWithScheduled(int scheduleCoreNum) {
        return new ThreadManager(ExecutorType.THREADPOOL.getExecutor(), scheduleCoreNum);
    }

    public static ThreadManager commonThreadManagerWithScheduled(int scheduleCoreNum, ThreadFactory scheduleThreadFactory) {
        return new ThreadManager(ExecutorType.THREADPOOL.getExecutor(), scheduleCoreNum, scheduleThreadFactory);
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

    //--------------------------------------------------------------------------------------------
    private enum ExecutorType {
        /**
         * ForkJoin线程池
         */
        FORKJOIN("forkjoin") {
            @Override
            public ExecutorService getExecutor() {
                return new ForkJoinPool();
            }
        },
        /**
         * 普通线程池
         */
        THREADPOOL("threadpool") {
            @Override
            public ExecutorService getExecutor() {
                return new ThreadPoolExecutor(0, SysUtils.getSuitableThreadNum(), 60L, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<>(), new SimpleThreadFactory("default-thread-manager"));
            }
        };
        private String name;

        ExecutorType(String name) {
            this.name = name;
        }

        abstract ExecutorService getExecutor();

        static ExecutorType getByName(String name) {
            for (ExecutorType type : values()) {
                if (type.getName().equals(name.toLowerCase())) {
                    return type;
                }
            }

            throw new UnknownExecutorTypeException("unknown executor type '" + name + "'");
        }

        String getName() {
            return name;
        }

        private static class UnknownExecutorTypeException extends RuntimeException {
            public UnknownExecutorTypeException(String message) {
                super(message);
            }
        }
    }
}
