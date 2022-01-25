package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.*;

/**
 * 利用Message的某种属性将Message分区,从而达到同一类的Message按顺序在同一线程执行
 *
 * @author huangjianqin
 * @date 2020-05-18
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class DefaultPartitionExecutor<KEY> implements ScheduledPartitionExecutor<KEY> {
    /** 分区数 */
    private final int partitionNum;
    /** 线程池 */
    private final FixOrderedEventLoopGroup eventLoopGroup;
    /** 分区算法 */
    private final Partitioner<KEY> partitioner;
    /** 是否stopped */
    private volatile boolean stopped;

    public DefaultPartitionExecutor() {
        this(SysUtils.CPU_NUM);
    }

    public DefaultPartitionExecutor(int partitionNum) {
        this(partitionNum, EfficientHashPartitioner.INSTANCE);
    }

    public DefaultPartitionExecutor(int partitionNum, Partitioner<KEY> partitioner) {
        this(partitionNum, partitioner, "partition-executor");
    }

    public DefaultPartitionExecutor(int partitionNum, Partitioner<KEY> partitioner, String workerNamePrefix) {
        this(partitionNum, partitioner, ExecutionContext.fix(partitionNum, workerNamePrefix, 3));
    }

    public DefaultPartitionExecutor(int partitionNum, Partitioner<KEY> partitioner, ExecutionContext ec) {
        Preconditions.checkArgument(partitionNum > 0, "partitionNum field must be greater then 0");

        this.partitionNum = partitionNum;
        this.eventLoopGroup = new FixOrderedEventLoopGroup(partitionNum, ec, OrderedEventLoop::new);
        this.partitioner = partitioner;
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 获取分区key
     */
    private int getPartitionId(KEY key) {
        return partitioner.toPartition(key, partitionNum);
    }

    @Override
    public void execute(KEY key, Runnable task) {
        submit(key, task, null);
    }

    @Override
    public <T> Future<T> submit(KEY key, Runnable task, T value) {
        return submit(key, Executors.callable(task, value));
    }

    @Override
    public <T> Future<T> submit(KEY key, Callable<T> task) {
        if (isTerminated()) {
            throw new IllegalStateException("executor is stopped");
        }
        FutureTask<T> futureTask = new FutureTask<>(task);
        eventLoopGroup.next(getPartitionId(key)).receive((e) -> futureTask.run());
        return futureTask;
    }

    @Override
    public boolean isTerminated() {
        return stopped;
    }

    @Override
    public void shutdown() {
        if (!isTerminated()) {
            stopped = true;
            eventLoopGroup.shutdown();
        }
    }

    @Override
    public ScheduledFuture<?> schedule(KEY key, Runnable task, long delay, TimeUnit unit) {
        if (isTerminated()) {
            throw new IllegalStateException("executor is stopped");
        }
        return eventLoopGroup.next(getPartitionId(key)).schedule((e) -> execute(key, task), delay, unit);
    }

    @Override
    public <V> Future<V> schedule(KEY key, Callable<V> callable, long delay, TimeUnit unit) {
        if (isTerminated()) {
            throw new IllegalStateException("executor is stopped");
        }

        FutureTask<V> futureTask = new FutureTask<V>(callable);
        eventLoopGroup.next(getPartitionId(key)).schedule((e) -> futureTask.run(), delay, unit);
        return futureTask;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(KEY key, Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (isTerminated()) {
            throw new IllegalStateException("executor is stopped");
        }
        return eventLoopGroup.next(getPartitionId(key)).scheduleAtFixedRate((e) -> execute(key, task), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(KEY key, Runnable task, long initialDelay, long delay, TimeUnit unit) {
        if (isTerminated()) {
            throw new IllegalStateException("executor is stopped");
        }
        return eventLoopGroup.next(getPartitionId(key)).scheduleWithFixedDelay((e) -> execute(key, task), initialDelay, delay, unit);
    }

}
