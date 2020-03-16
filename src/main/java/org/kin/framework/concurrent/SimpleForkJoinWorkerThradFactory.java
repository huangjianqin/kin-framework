package org.kin.framework.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020-03-16
 */
public class SimpleForkJoinWorkerThradFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private final AtomicInteger counter;
    private final String namePrefix;


    public SimpleForkJoinWorkerThradFactory(String namePrefix) {
        this.namePrefix = namePrefix + "--thread-";
        this.counter = new AtomicInteger(1);
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread workerThread = new SimpleForkJoinWorkerThread(pool);
        workerThread.setName(this.namePrefix + counter.getAndIncrement());
        return workerThread;
    }
}
