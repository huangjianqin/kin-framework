package org.kin.framework.concurrent;

import io.micrometer.core.instrument.Metrics;

import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/10/15
 */
public class ForkJoinPoolWithMetric extends ForkJoinPoolWithLog {
    public ForkJoinPoolWithMetric(String name) {
        super(name);
    }

    public ForkJoinPoolWithMetric(int parallelism, String name) {
        super(parallelism, name);
    }

    public ForkJoinPoolWithMetric(int parallelism, ForkJoinWorkerThreadFactory factory, Thread.UncaughtExceptionHandler handler, boolean asyncMode, String name) {
        super(parallelism, factory, handler, asyncMode, name);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new RunnableFutureProxy<>(super.newTaskFor(runnable, value));
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new RunnableFutureProxy<>(super.newTaskFor(callable));
    }

    //----------------------------------
    private class RunnableFutureProxy<T> implements RunnableFuture<T> {
        private final RunnableFuture<T> proxy;

        public RunnableFutureProxy(RunnableFuture<T> proxy) {
            this.proxy = proxy;
        }

        @Override
        public void run() {
            Metrics.timer("forkJoinPool." + getName()).record(proxy);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return proxy.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return proxy.isCancelled();
        }

        @Override
        public boolean isDone() {
            return proxy.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return proxy.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return proxy.get(timeout, unit);
        }
    }
}
