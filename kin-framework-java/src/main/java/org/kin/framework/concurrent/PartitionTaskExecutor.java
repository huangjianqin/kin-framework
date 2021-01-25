package org.kin.framework.concurrent;

import java.util.concurrent.*;

/**
 * 利用Message的某种属性将Message分区,从而达到同一类的Message按顺序在同一线程执行
 * 仅仅处理Runnable或者Callable, 不区分不同的Receiver实例
 *
 * @author huangjianqin
 * @date 2020-05-18
 */
public class PartitionTaskExecutor<KEY> extends PartitionDispatcher<KEY, FutureTask<?>> {

    public PartitionTaskExecutor() {
        super();
    }

    public PartitionTaskExecutor(int partitionNum) {
        super(partitionNum);
    }

    public PartitionTaskExecutor(int partitionNum, Partitioner<KEY> partitioner) {
        super(partitionNum, partitioner);
    }

    public PartitionTaskExecutor(int partitionNum, Partitioner<KEY> partitioner, String workerNamePrefix) {
        super(partitionNum, partitioner, workerNamePrefix);
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    protected Receiver<FutureTask<?>> receiver() {
        return PartitionTaskReceiver.INSTANCE;
    }

    public Future<?> execute(KEY key, Runnable task) {
        return execute(key, task, null);
    }

    public <T> Future<T> execute(KEY key, Runnable task, T value) {
        return execute(key, Executors.callable(task, value));
    }

    public <T> Future<T> execute(KEY key, Callable<T> task) {
        FutureTask<T> futureTask = new FutureTask<>(task);
        postMessage(key, futureTask);
        return futureTask;
    }

    public ScheduledFuture<?> schedule(KEY key, Runnable task, long delay, TimeUnit unit) {
        return executionContext().schedule(() -> execute(key, task), delay, unit);
    }

    public <V> ScheduledFuture<V> schedule(KEY key, Callable<V> callable, long delay, TimeUnit unit) {
        return executionContext().schedule(() -> execute(key, callable).get(), delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(KEY key, Runnable task, long initialDelay, long period, TimeUnit unit) {
        return executionContext().scheduleAtFixedRate(() -> execute(key, task), initialDelay, period, unit);
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(KEY key, Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return executionContext().scheduleWithFixedDelay(() -> execute(key, task), initialDelay, delay, unit);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 处理Runnable或者Callable的Receiver实现
     * 单例模式
     */
    private static class PartitionTaskReceiver extends Receiver<FutureTask<?>> {
        private static final PartitionTaskReceiver INSTANCE = new PartitionTaskReceiver();

        @Override
        public void receive(FutureTask task) {
            task.run();
        }
    }
}
