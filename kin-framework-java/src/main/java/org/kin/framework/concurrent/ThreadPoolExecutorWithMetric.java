package org.kin.framework.concurrent;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/10/14
 */
public class ThreadPoolExecutorWithMetric extends ThreadPoolExecutorWithLog {
    private static final ThreadLocal<Timer.Sample> TIMER_SAMPLE_THREAD_LOCAL = new ThreadLocal<>();

    public ThreadPoolExecutorWithMetric(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, name);
    }

    public ThreadPoolExecutorWithMetric(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, name);
    }

    public ThreadPoolExecutorWithMetric(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, java.util.concurrent.RejectedExecutionHandler handler, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler, name);
    }

    public ThreadPoolExecutorWithMetric(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                        RejectedExecutionHandler handler, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler, name);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        TIMER_SAMPLE_THREAD_LOCAL.set(Timer.start(Metrics.globalRegistry));
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        Timer.Sample sample = TIMER_SAMPLE_THREAD_LOCAL.get();
        if (Objects.nonNull(sample)) {
            sample.stop(Metrics.timer("threadPool." + getName()));
            TIMER_SAMPLE_THREAD_LOCAL.remove();
        }
    }
}