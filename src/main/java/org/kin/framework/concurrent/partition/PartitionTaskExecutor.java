package org.kin.framework.concurrent.partition;

import org.kin.framework.concurrent.actor.Receiver;
import org.kin.framework.concurrent.partition.partitioner.Partitioner;

import java.util.concurrent.*;

/**
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
    private class PartitionTask implements Runnable {
        private final KEY key;
        private final Runnable target;

        PartitionTask(KEY key, Runnable target) {
            this.key = key;
            this.target = target;
        }

        @Override
        public void run() {
            target.run();
        }
    }

    private static class PartitionTaskReceiver extends Receiver<FutureTask<?>> {
        private static final PartitionTaskReceiver INSTANCE = new PartitionTaskReceiver();

        @Override
        public void receive(FutureTask task) {
            task.run();
        }
    }
}
