package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/10/14
 */
public final class ThreadPoolUtils {
    /**
     * The default rejected execution handler
     */
    private static final java.util.concurrent.RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = new ThreadPoolExecutor.AbortPolicy();

    private ThreadPoolUtils() {
    }

    public static TheadPoolBuilder threadPoolBuilder() {
        return new TheadPoolBuilder();
    }

    public static TheadPoolBuilder eagerThreadPoolBuilder() {
        return new TheadPoolBuilder().eager();
    }

    public static ScheduledThreadPoolBuilder scheduledThreadPoolBuilder() {
        return new ScheduledThreadPoolBuilder();
    }

    public static ThreadPoolExecutor newThreadPool(String poolName, boolean enableMetric,
                                                   int coreThreads, int maximumThreads,
                                                   long keepAliveTime, TimeUnit unit,
                                                   BlockingQueue<Runnable> workQueue,
                                                   ThreadFactory threadFactory) {
        return newThreadPool(poolName, enableMetric, coreThreads, maximumThreads, keepAliveTime, unit, workQueue,
                threadFactory, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public static ThreadPoolExecutor newThreadPool(String poolName, boolean enableMetric,
                                                   int coreThreads, int maximumThreads,
                                                   long keepAliveTime, TimeUnit unit,
                                                   BlockingQueue<Runnable> workQueue,
                                                   ThreadFactory threadFactory,
                                                   java.util.concurrent.RejectedExecutionHandler rejectedHandler) {
        if (enableMetric) {
            return new ThreadPoolExecutorWithMetric(coreThreads, maximumThreads, keepAliveTime, unit, workQueue,
                    threadFactory, rejectedHandler, poolName);
        } else {
            return new ThreadPoolExecutorWithLog(coreThreads, maximumThreads, keepAliveTime, unit, workQueue,
                    threadFactory, rejectedHandler, poolName);
        }
    }

    public static ThreadPoolExecutor newEagerThreadPool(String poolName, boolean enableMetric,
                                                        int coreThreads, int maximumThreads,
                                                        long keepAliveTime, TimeUnit unit,
                                                        int queueSize,
                                                        ThreadFactory threadFactory) {
        return newEagerThreadPool(poolName, enableMetric, coreThreads, maximumThreads,
                keepAliveTime, unit, queueSize, threadFactory, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public static ThreadPoolExecutor newEagerThreadPool(String poolName, boolean enableMetric,
                                                        int coreThreads, int maximumThreads,
                                                        long keepAliveTime, TimeUnit unit,
                                                        int queueSize,
                                                        ThreadFactory threadFactory,
                                                        java.util.concurrent.RejectedExecutionHandler rejectedHandler) {
        if (enableMetric) {
            return EagerThreadPoolExecutorWithMetric.create(poolName, coreThreads, maximumThreads, keepAliveTime, unit, queueSize,
                    threadFactory, rejectedHandler);
        } else {
            return EagerThreadPoolExecutorWithLog.create(poolName, coreThreads, maximumThreads, keepAliveTime, unit, queueSize,
                    threadFactory, rejectedHandler);
        }
    }

    public static ScheduledThreadPoolExecutor newScheduledThreadPool(String poolName, boolean enableMetric,
                                                                     int coreThreads,
                                                                     ThreadFactory threadFactory) {
        return newScheduledThreadPool(poolName, enableMetric, coreThreads, threadFactory, DEFAULT_REJECTED_EXECUTION_HANDLER);
    }

    public static ScheduledThreadPoolExecutor newScheduledThreadPool(String poolName, boolean enableMetric,
                                                                     int coreThreads,
                                                                     ThreadFactory threadFactory,
                                                                     java.util.concurrent.RejectedExecutionHandler rejectedHandler) {
        if (enableMetric) {
            return new ScheduledThreadPoolExecutorWithMetric(coreThreads, threadFactory, rejectedHandler, poolName);
        } else {
            return new ScheduledThreadPoolExecutorWithLog(coreThreads, threadFactory, rejectedHandler, poolName);
        }
    }

    //------------------------------------------------------------------------------------
    public static final class TheadPoolBuilder {
        private String poolName = "undefined";
        private boolean enableMetric;
        private int coreThreads = SysUtils.getSuitableThreadNum();
        private int maximumThreads = Integer.MAX_VALUE;
        private long keepAliveTime = 60L;
        private TimeUnit unit = TimeUnit.SECONDS;
        private BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
        private ThreadFactory threadFactory = Executors.defaultThreadFactory();
        private java.util.concurrent.RejectedExecutionHandler handler = ThreadPoolUtils.DEFAULT_REJECTED_EXECUTION_HANDLER;
        /** 是否使用{@link EagerThreadPoolExecutor} */
        private boolean eager;
        /** eager=true下有效, eager队列大小, 0表示不限制 */
        private int queueSize;

        private TheadPoolBuilder() {
        }

        public TheadPoolBuilder poolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        public TheadPoolBuilder metric() {
            this.enableMetric = true;
            return this;
        }

        public TheadPoolBuilder coreThreads(int coreThreads) {
            this.coreThreads = coreThreads;
            return this;
        }

        public TheadPoolBuilder maximumThreads(int maximumThreads) {
            this.maximumThreads = maximumThreads;
            return this;
        }

        public TheadPoolBuilder keepAliveSeconds(long keepAliveSeconds) {
            return keepAlive(keepAliveSeconds, TimeUnit.SECONDS);
        }

        public TheadPoolBuilder keepAlive(long keepAliveTime, TimeUnit unit) {
            this.keepAliveTime = keepAliveTime;
            this.unit = unit;
            return this;
        }

        public TheadPoolBuilder workQueue(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            return this;
        }

        public TheadPoolBuilder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            if (threadFactory instanceof SimpleThreadFactory) {
                SimpleThreadFactory simpleThreadFactory = (SimpleThreadFactory) threadFactory;
                this.poolName = simpleThreadFactory.getPrefix();
            }
            return this;
        }

        public TheadPoolBuilder rejectedHandler(java.util.concurrent.RejectedExecutionHandler handler) {
            this.handler = handler;
            return this;
        }

        public TheadPoolBuilder eager() {
            this.eager = true;
            return this;
        }

        public TheadPoolBuilder eager(int queueSize) {
            this.eager = true;
            this.queueSize = queueSize;
            return this;
        }

        public ThreadPoolExecutor build() {
            Preconditions.checkNotNull(this.coreThreads, "coreThreads");
            Preconditions.checkNotNull(this.maximumThreads, "maximumThreads");
            Preconditions.checkNotNull(this.keepAliveTime, "keepAliveTime");
            Preconditions.checkNotNull(this.unit, "timeunit");
            Preconditions.checkNotNull(this.workQueue, "workQueue");
            Preconditions.checkNotNull(this.threadFactory, "threadFactory");
            Preconditions.checkNotNull(this.handler, "handler");

            if (eager) {
                return ThreadPoolUtils.newEagerThreadPool(this.poolName, this.enableMetric, this.coreThreads,
                        this.maximumThreads, this.keepAliveTime, this.unit, this.queueSize, this.threadFactory, this.handler);
            } else {
                return ThreadPoolUtils.newThreadPool(this.poolName, this.enableMetric, this.coreThreads,
                        this.maximumThreads, this.keepAliveTime, this.unit, this.workQueue, this.threadFactory, this.handler);
            }
        }
    }

    public static final class ScheduledThreadPoolBuilder {
        private String poolName = "undefined";
        private boolean enableMetric;
        private int coreThreads = SysUtils.getSuitableThreadNum();
        private ThreadFactory threadFactory = Executors.defaultThreadFactory();
        private java.util.concurrent.RejectedExecutionHandler handler = ThreadPoolUtils.DEFAULT_REJECTED_EXECUTION_HANDLER;

        private ScheduledThreadPoolBuilder() {
        }

        public ScheduledThreadPoolBuilder poolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        public ScheduledThreadPoolBuilder metric() {
            this.enableMetric = true;
            return this;
        }

        public ScheduledThreadPoolBuilder coreThreads(int coreThreads) {
            this.coreThreads = coreThreads;
            return this;
        }

        public ScheduledThreadPoolBuilder threadFactory(ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            if (threadFactory instanceof SimpleThreadFactory) {
                SimpleThreadFactory simpleThreadFactory = (SimpleThreadFactory) threadFactory;
                this.poolName = simpleThreadFactory.getPrefix();
            }
            return this;
        }

        public ScheduledThreadPoolBuilder rejectedHandler(RejectedExecutionHandler handler) {
            this.handler = handler;
            return this;
        }

        public ScheduledThreadPoolExecutor build() {
            Preconditions.checkNotNull(this.coreThreads, "coreThreads");
            Preconditions.checkNotNull(this.threadFactory, "threadFactory");
            Preconditions.checkNotNull(this.handler, "handler");

            return ThreadPoolUtils.newScheduledThreadPool(this.poolName, this.enableMetric, this.coreThreads,
                    this.threadFactory, this.handler);
        }
    }
}
