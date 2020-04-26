package org.kin.framework.actor;

import org.kin.framework.concurrent.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2020-04-26
 */
public abstract class AbstractDispatcher<KEY, MSG> implements Dispatcher<KEY, MSG> {
    private static final Logger log = LoggerFactory.getLogger(AbstractDispatcher.class);

    protected ExecutionContext executionContext;
    protected volatile boolean stopped;

    public AbstractDispatcher(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public abstract void doInit();

    public abstract void doClose();

    public abstract void doRegister(KEY key, Receiver<MSG> receiver, boolean enableConcurrent);

    public abstract void doUnRegister(KEY key);

    public abstract void doPostMessage(KEY key, MSG message);

    @Override
    public final void init() {
        synchronized (this) {
            if (!stopped) {
                doInit();
            }
        }
    }

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
    public final void unRegister(KEY key) {
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

    public boolean isStopped() {
        return stopped;
    }
}
