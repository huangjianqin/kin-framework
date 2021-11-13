package org.kin.framework.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/3/10
 */
public class SingleThreadEventLoopGroup extends SingleThreadEventExecutorPool implements EventLoopGroup<SingleThreadEventLoop> {
    public SingleThreadEventLoopGroup(int coreSize) {
        super(coreSize);
    }

    public SingleThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser) {
        super(coreSize, chooser);
    }

    public SingleThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser, String workerNamePrefix) {
        super(coreSize, chooser, workerNamePrefix);
    }

    public SingleThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser, ThreadFactory threadFactory) {
        super(coreSize, chooser, threadFactory);
    }

    public SingleThreadEventLoopGroup(int coreSize, EventExecutorChooser chooser, ExecutorService executor) {
        super(coreSize, chooser, executor);
    }

    public SingleThreadEventLoopGroup(int coreSize, String workerNamePrefix) {
        super(coreSize, workerNamePrefix);
    }

    public SingleThreadEventLoopGroup(int coreSize, ThreadFactory threadFactory) {
        super(coreSize, threadFactory);
    }

    public SingleThreadEventLoopGroup(int coreSize, ExecutorService executor) {
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
