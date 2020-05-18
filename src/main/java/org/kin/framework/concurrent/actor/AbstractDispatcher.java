package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2020-04-26
 */
public abstract class AbstractDispatcher<KEY, MSG> implements Dispatcher<KEY, MSG> {
    private static final Logger log = LoggerFactory.getLogger(AbstractDispatcher.class);

    /** 底层线程池 */
    protected ExecutionContext executionContext;
    /** Dispatcher是否stopped */
    protected volatile boolean stopped;

    public AbstractDispatcher(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    protected abstract void doClose();

    protected abstract void doRegister(KEY key, Receiver<MSG> receiver, boolean enableConcurrent);

    protected abstract void doUnRegister(KEY key);

    protected abstract void doPostMessage(KEY key, MSG message);

    @Override
    public final void register(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        synchronized (this) {
            if (stopped) {
                throw new IllegalStateException("dispatcher has been stopped");
            }

            doRegister(key, receiver, enableConcurrent);
        }
    }

    @Override
    public final void unregister(KEY key) {
        if (stopped) {
            return;
        }

        doUnRegister(key);
    }

    @Override
    public final void postMessage(KEY key, MSG message) {
        synchronized (this) {
            if (stopped) {
                return;
            }

            doPostMessage(key, message);
        }
    }

    @Override
    public final void shutdown() {
        close();
    }

    @Override
    public final void close() {
        synchronized (this) {
            if (stopped) {
                return;
            }

            stopped = true;

            doClose();

            executionContext.shutdown();
        }
    }

    @Override
    public final ExecutionContext executionContext() {
        return executionContext;
    }

    public final boolean isStopped() {
        return stopped;
    }
}
