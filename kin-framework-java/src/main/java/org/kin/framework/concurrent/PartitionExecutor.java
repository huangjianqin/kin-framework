package org.kin.framework.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author huangjianqin
 * @date 2021/1/26
 */
public interface PartitionExecutor<KEY> {
    /**
     * 在{@link ExecutorService} execute(Runnable)基础上, 扩展根据分区key指定分区executor执行command
     */
    void execute(KEY key, Runnable task);

    /**
     * 在{@link ExecutorService} submit(Runnable, T)基础上, 扩展根据分区key指定分区executor执行command
     */
    <T> Future<T> submit(KEY key, Runnable task, T value);

    /**
     * 在{@link ExecutorService} submit(Callable)基础上, 扩展根据分区key指定分区executor执行command
     */
    <T> Future<T> submit(KEY key, Callable<T> task);

    /**
     * @return 是否terminated
     */
    boolean isTerminated();

    /**
     * shutdown
     */
    void shutdown();
}
