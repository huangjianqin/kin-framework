package org.kin.framework.concurrent;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-04-26
 */
public abstract class AbstractDispatcher<KEY, MSG> implements Dispatcher<KEY, MSG> {
    /** 底层线程池 */
    protected ExecutionContext executionContext;
    /** Dispatcher是否stopped */
    protected volatile boolean stopped;

    public AbstractDispatcher(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * 处理close逻辑
     */
    protected abstract void doClose();

    @Override
    public final void schedule(KEY key, MSG message, long delay, TimeUnit unit) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(message)) {
            throw new IllegalArgumentException("arg 'key' or 'message' is null");
        }

        if (executionContext.withSchedule()) {
            executionContext.schedule(() -> postMessage(key, message), delay, unit);
        } else {
            throw new UnsupportedOperationException("underline ExecutionContext doesn't support scheduled");
        }
    }

    @Override
    public final void scheduleAtFixedRate(KEY key, MSG message, long initialDelay, long period, TimeUnit unit) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(message)) {
            throw new IllegalArgumentException("arg 'key' or 'message' is null");
        }

        if (executionContext.withSchedule()) {
            executionContext.scheduleAtFixedRate(() -> postMessage(key, message), initialDelay, period, unit);
        } else {
            throw new UnsupportedOperationException("underline ExecutionContext doesn't support scheduled");
        }
    }

    @Override
    public final void scheduleWithFixedDelay(KEY key, MSG message, long initialDelay, long delay, TimeUnit unit) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(message)) {
            throw new IllegalArgumentException("arg 'key' or 'message' is null");
        }

        if (executionContext.withSchedule()) {
            executionContext.scheduleWithFixedDelay(() -> postMessage(key, message), initialDelay, delay, unit);
        } else {
            throw new UnsupportedOperationException("underline ExecutionContext doesn't support scheduled");
        }
    }

    @Override
    public final void shutdown() {
        close();
    }

    @Override
    public final void close() {
        if (stopped) {
            throw new IllegalStateException("dispatcher is closed");
        }
        stopped = true;
        doClose();
        executionContext.shutdown();
        //help gc
        executionContext = null;
    }

    @Override
    public ExecutionContext executionContext() {
        return executionContext;
    }

    public final boolean isStopped() {
        return stopped;
    }
}
