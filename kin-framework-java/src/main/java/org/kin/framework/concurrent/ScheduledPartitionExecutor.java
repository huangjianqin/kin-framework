package org.kin.framework.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/1/26
 */
public interface ScheduledPartitionExecutor<KEY> extends PartitionExecutor<KEY> {
    /**
     * 在{@link java.util.concurrent.ScheduledExecutorService} schedule(Runnable, long, TimeUnit)基础上,
     * 扩展根据分区key指定分区executor执行schedule command
     */
    ScheduledFuture<?> schedule(KEY key, Runnable task, long delay, TimeUnit unit);

    /**
     * 在{@link java.util.concurrent.ScheduledExecutorService} schedule(callable, long, TimeUnit)基础上,
     * 扩展根据分区key指定分区executor执行schedule command
     */
    <V> Future<V> schedule(KEY key, Callable<V> callable, long delay, TimeUnit unit);

    /**
     * 在{@link java.util.concurrent.ScheduledExecutorService} scheduleAtFixedRate(Runnable, long, long, TimeUnit)基础上,
     * 扩展根据分区key指定分区executor执行schedule command
     */
    ScheduledFuture<?> scheduleAtFixedRate(KEY key, Runnable task, long initialDelay, long period, TimeUnit unit);

    /**
     * 在{@link java.util.concurrent.ScheduledExecutorService} scheduleWithFixedDelay(Runnable, long, long, TimeUnit)基础上,
     * 扩展根据分区key指定分区executor执行schedule command
     */
    ScheduledFuture<?> scheduleWithFixedDelay(KEY key, Runnable task, long initialDelay, long delay, TimeUnit unit);
}
