package org.kin.framework.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
public class MultiThreadEventExecutorPool extends AbstractEventExecutorPool {
    public MultiThreadEventExecutorPool(int coreSize) {
        super(coreSize);
    }

    public MultiThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser) {
        super(coreSize, chooser);
    }

    public MultiThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser, String workerNamePrefix) {
        super(coreSize, chooser, workerNamePrefix);
    }

    public MultiThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser, ThreadFactory threadFactory) {
        super(coreSize, chooser, threadFactory);
    }

    public MultiThreadEventExecutorPool(int coreSize, EventExecutorChooser chooser, ExecutorService executor) {
        super(coreSize, chooser, executor);
    }

    public MultiThreadEventExecutorPool(int coreSize, String workerNamePrefix) {
        super(coreSize, workerNamePrefix);
    }

    public MultiThreadEventExecutorPool(int coreSize, ThreadFactory threadFactory) {
        super(coreSize, threadFactory);
    }

    public MultiThreadEventExecutorPool(int coreSize, ExecutorService executor) {
        super(coreSize, executor);
    }

    @Override
    protected EventExecutor newEventExecutor(ExecutorService executor) {
        return new SingleThreadEventExecutor(this, executor);
    }
}
