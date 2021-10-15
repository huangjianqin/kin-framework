package org.kin.framework.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/10/15
 */
public class EagerThreadPoolExecutorWithLog extends EagerThreadPoolExecutor {
    private static final Logger log = LoggerFactory.getLogger(EagerThreadPoolExecutorWithLog.class);
    private final String name;

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit) {
        return create(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, 0);
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        RejectedExecutionHandler handler) {
        return create(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, 0, handler);
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        ThreadFactory threadFactory) {
        return create(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, 0, threadFactory);
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        ThreadFactory threadFactory,
                                                        RejectedExecutionHandler handler) {
        return create(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, 0, threadFactory, handler);
    }

    private static EagerTaskQueue<Runnable> create(int queueSize) {
        return queueSize > 0 ? new EagerTaskQueue<>(queueSize) : new EagerTaskQueue<>();
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        int queueSize) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutorWithLog executor = new EagerThreadPoolExecutorWithLog(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, name);
        workQueue.updateExecutor(executor);
        return executor;
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        int queueSize,
                                                        RejectedExecutionHandler handler) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutorWithLog executor = new EagerThreadPoolExecutorWithLog(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler, name);
        workQueue.updateExecutor(executor);
        return executor;
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        int queueSize,
                                                        ThreadFactory threadFactory) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutorWithLog executor = new EagerThreadPoolExecutorWithLog(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, name);
        workQueue.updateExecutor(executor);
        return executor;
    }

    public static EagerThreadPoolExecutorWithLog create(String name,
                                                        int corePoolSize,
                                                        int maximumPoolSize,
                                                        long keepAliveTime,
                                                        TimeUnit unit,
                                                        int queueSize,
                                                        ThreadFactory threadFactory,
                                                        RejectedExecutionHandler handler) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutorWithLog executor = new EagerThreadPoolExecutorWithLog(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler, name);
        workQueue.updateExecutor(executor);
        return executor;
    }

    protected EagerThreadPoolExecutorWithLog(int corePoolSize, int maximumPoolSize,
                                             long keepAliveTime, TimeUnit unit,
                                             EagerTaskQueue<Runnable> workQueue, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, null);
        this.name = name;
    }

    protected EagerThreadPoolExecutorWithLog(int corePoolSize, int maximumPoolSize,
                                             long keepAliveTime, TimeUnit unit,
                                             EagerTaskQueue<Runnable> workQueue, RejectedExecutionHandler handler,
                                             String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, null);
        this.name = name;
    }

    protected EagerThreadPoolExecutorWithLog(int corePoolSize, int maximumPoolSize,
                                             long keepAliveTime, TimeUnit unit,
                                             EagerTaskQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                             String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, threadFactory);
        this.name = name;
    }

    protected EagerThreadPoolExecutorWithLog(int corePoolSize, int maximumPoolSize,
                                             long keepAliveTime, TimeUnit unit,
                                             EagerTaskQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                             RejectedExecutionHandler handler, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, threadFactory);
        this.name = name;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> f = (Future<?>) r;
                if (f.isDone()) {
                    f.get();
                }
            } catch (final CancellationException ce) {
                // ignored
            } catch (final ExecutionException ee) {
                t = ee.getCause();
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            log.error("uncaught exception in pool: {}, {}.", getName(), super.toString(), t);
        }
    }

    @Override
    protected void terminated() {
        super.terminated();
        log.info("ThreadPool is terminated: {}, {}.", getName(), super.toString());
    }

    //getter
    public String getName() {
        return name;
    }
}