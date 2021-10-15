package org.kin.framework.concurrent;

import org.kin.framework.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/10/15
 */
public class ForkJoinPoolWithLog extends ForkJoinPool {
    private static final Logger log = LoggerFactory.getLogger(ForkJoinPoolWithLog.class);

    private final String name;

    public ForkJoinPoolWithLog(String name) {
        this.name = name;
    }

    public ForkJoinPoolWithLog(int parallelism, String name) {
        super(parallelism);
        this.name = name;
    }

    public ForkJoinPoolWithLog(int parallelism, ForkJoinWorkerThreadFactory factory,
                               Thread.UncaughtExceptionHandler handler, boolean asyncMode,
                               String name) {
        super(parallelism, factory, handler, asyncMode);
        if (StringUtils.isBlank(name) && factory instanceof SimpleForkJoinWorkerThreadFactory) {
            SimpleForkJoinWorkerThreadFactory simpleForkJoinWorkerThreadFactory = (SimpleForkJoinWorkerThreadFactory) factory;
            name = simpleForkJoinWorkerThreadFactory.getNamePrefix();
        }
        if (StringUtils.isBlank(name)) {
            name = "undefined";
        }
        this.name = name;
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new RunnableFutureProxy<>(super.newTaskFor(runnable, value));
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new RunnableFutureProxy<>(super.newTaskFor(callable));
    }

    @Override
    public void shutdown() {
        super.shutdown();
        log.info("ForkJoinPool is terminated: {}, {}.", getName(), super.toString());
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> runnables = super.shutdownNow();
        log.info("ForkJoinPool is terminated: {}, {}.", getName(), super.toString());
        return runnables;
    }

    //getter
    public String getName() {
        return name;
    }

    //----------------------------------
    private class RunnableFutureProxy<T> implements RunnableFuture<T> {
        private final RunnableFuture<T> proxy;

        public RunnableFutureProxy(RunnableFuture<T> proxy) {
            this.proxy = proxy;
        }

        @Override
        public void run() {
            Throwable t = null;
            try {
                proxy.run();
            } catch (Throwable throwable) {
                t = throwable;
            } finally {
                if (t != null) {
                    log.error("uncaught exception in pool: {}, {}.", getName(), super.toString(), t);
                }
            }
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
