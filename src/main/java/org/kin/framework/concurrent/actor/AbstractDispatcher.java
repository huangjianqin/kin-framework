package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

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

    private void sync(Runnable func) {
        synchronized (this) {
            if (stopped) {
                return;
            }

            func.run();
        }
    }

    @Override
    public final void unregister(KEY key) {
        sync(() -> doUnRegister(key));
    }

    @Override
    public final void postMessage(KEY key, MSG message) {
        sync(() -> doPostMessage(key, message));
    }

    @Override
    public void schedule(KEY key, MSG message, long delay, TimeUnit unit) {
        sync(() -> {
            if (executionContext.withSchedule()) {
                executionContext.schedule(() -> postMessage(key, message), delay, unit);
            }
        });
    }

    @Override
    public void scheduleAtFixedRate(KEY key, MSG message, long initialDelay, long period, TimeUnit unit) {
        sync(() -> {
            if (executionContext.withSchedule()) {
                executionContext.scheduleAtFixedRate(() -> postMessage(key, message), initialDelay, period, unit);
            }
        });
    }

    @Override
    public void scheduleWithFixedDelay(KEY key, MSG message, long initialDelay, long delay, TimeUnit unit) {
        sync(() -> {
            if (executionContext.withSchedule()) {
                executionContext.scheduleWithFixedDelay(() -> postMessage(key, message), initialDelay, delay, unit);
            }
        });
    }

    @Override
    public final void shutdown() {
        close();
    }

    @Override
    public final void close() {
        sync(() -> {
            stopped = true;

            doClose();

            executionContext.shutdown();
        });
    }

    @Override
    public final ExecutionContext executionContext() {
        return executionContext;
    }

    public final boolean isStopped() {
        return stopped;
    }
}
