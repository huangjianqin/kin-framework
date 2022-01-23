package org.kin.framework.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/3/10
 */
public class MultiThreadEventLoopGroup extends MultiThreadEventExecutorPool implements EventLoopGroup<SingleThreadEventLoop> {
    public MultiThreadEventLoopGroup(int coreSize) {
        super(coreSize);
    }

    public MultiThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser) {
        super(coreSize, chooser);
    }

    public MultiThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser, String workerNamePrefix) {
        super(coreSize, chooser, workerNamePrefix);
    }

    public MultiThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser, ThreadFactory threadFactory) {
        super(coreSize, chooser, threadFactory);
    }

    public MultiThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser, ExecutorService executor) {
        super(coreSize, chooser, executor);
    }

    public MultiThreadEventLoopGroup(int coreSize, String workerNamePrefix) {
        super(coreSize, workerNamePrefix);
    }

    public MultiThreadEventLoopGroup(int coreSize, ThreadFactory threadFactory) {
        super(coreSize, threadFactory);
    }

    public MultiThreadEventLoopGroup(int coreSize, ExecutorService executor) {
        super(coreSize, executor);
    }

    @Override
    public SingleThreadEventLoop next() {
        return (SingleThreadEventLoop) super.next();
    }

    @Override
    protected EventExecutor newEventExecutor(ExecutorService executor) {
        return new SingleThreadEventLoop(this, executor);
    }
}
