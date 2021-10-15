package org.kin.framework.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2021/10/14
 */
public class ScheduledThreadPoolExecutorWithLog extends ScheduledThreadPoolExecutor {
    private static final Logger log = LoggerFactory.getLogger(ScheduledThreadPoolExecutorWithLog.class);

    private final String name;

    public ScheduledThreadPoolExecutorWithLog(int corePoolSize, String name) {
        super(corePoolSize);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, null);
        this.name = name;
    }

    public ScheduledThreadPoolExecutorWithLog(int corePoolSize, ThreadFactory threadFactory, String name) {
        super(corePoolSize, threadFactory);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, threadFactory);
        this.name = name;
    }

    public ScheduledThreadPoolExecutorWithLog(int corePoolSize, RejectedExecutionHandler handler, String name) {
        super(corePoolSize, handler);
        name = ThreadPoolUtils.applyPoolNameIfBlank(name, null);
        this.name = name;
    }

    public ScheduledThreadPoolExecutorWithLog(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler, String name) {
        super(corePoolSize, threadFactory, handler);
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
        log.info("ScheduledThreadPool is terminated: {}, {}.", getName(), super.toString());
    }

    //getter
    public String getName() {
        return name;
    }
}