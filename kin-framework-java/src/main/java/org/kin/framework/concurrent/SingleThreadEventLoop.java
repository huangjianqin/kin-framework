package org.kin.framework.concurrent;

import java.util.concurrent.Executor;

/**
 * @author huangjianqin
 * @date 2021/3/10
 */
public class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop<SingleThreadEventLoop> {

    public SingleThreadEventLoop(EventLoopGroup<SingleThreadEventLoop> parent, Executor executor) {
        super(parent, executor);
    }

    public SingleThreadEventLoop(EventLoopGroup<SingleThreadEventLoop> parent, Executor executor, RejectedExecutionHandler rejectedExecutionHandler) {
        super(parent, executor, rejectedExecutionHandler);
    }

    @SuppressWarnings("unchecked")
    @Override
    public EventLoopGroup<SingleThreadEventLoop> parent() {
        return (EventLoopGroup<SingleThreadEventLoop>) super.parent();
    }
}
