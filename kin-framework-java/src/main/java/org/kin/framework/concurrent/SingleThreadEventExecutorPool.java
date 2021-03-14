package org.kin.framework.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * @author huangjianqin
 * @date 2021/1/26
 */
public class SingleThreadEventExecutorPool extends AbstractEventExecutorPool {
    public SingleThreadEventExecutorPool(int coreSize) {
        super(coreSize);
    }

    public SingleThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser) {
        super(coreSize, chooser);
    }

    public SingleThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser, String workerNamePrefix) {
        super(coreSize, chooser, workerNamePrefix);
    }

    public SingleThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser, ThreadFactory threadFactory) {
        super(coreSize, chooser, threadFactory);
    }

    public SingleThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser, ExecutorService executor) {
        super(coreSize, chooser, executor);
    }

    public SingleThreadEventExecutorPool(int coreSize, String workerNamePrefix) {
        super(coreSize, workerNamePrefix);
    }

    public SingleThreadEventExecutorPool(int coreSize, ThreadFactory threadFactory) {
        super(coreSize, threadFactory);
    }

    public SingleThreadEventExecutorPool(int coreSize, ExecutorService executor) {
        super(coreSize, executor);
    }

    @Override
    protected EventExecutor newEventExecutor(ExecutorService executor) {
        return new SingleThreadEventExecutor(this, executor);
    }
}
