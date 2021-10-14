package org.kin.framework.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link java.util.concurrent.ThreadPoolExecutor} 优先入队, 如果队列满了, 则创建worker
 * {@link EagerThreadPoolExecutor} 优先创建worker, 当无法创建新worker时, 才选择入队
 *
 * @author huangjianqin
 * @date 2021/4/19
 */
public class EagerThreadPoolExecutor extends ThreadPoolExecutor {
    /** 统计task count */
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit) {
        return create(corePoolSize, maximumPoolSize, keepAliveTime, unit, 0);
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 RejectedExecutionHandler handler) {
        return create(corePoolSize, maximumPoolSize, keepAliveTime, unit, 0, handler);
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 ThreadFactory threadFactory) {
        return create(corePoolSize, maximumPoolSize, keepAliveTime, unit, 0, threadFactory);
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 ThreadFactory threadFactory,
                                                 RejectedExecutionHandler handler) {
        return create(corePoolSize, maximumPoolSize, keepAliveTime, unit, 0, threadFactory, handler);
    }

    private static EagerTaskQueue<Runnable> create(int queueSize) {
        return queueSize > 0 ? new EagerTaskQueue<>(queueSize) : new EagerTaskQueue<>();
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 int queueSize) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        workQueue.updateExecutor(executor);
        return executor;
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 int queueSize,
                                                 RejectedExecutionHandler handler) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
        workQueue.updateExecutor(executor);
        return executor;
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 int queueSize,
                                                 ThreadFactory threadFactory) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        workQueue.updateExecutor(executor);
        return executor;
    }

    public static EagerThreadPoolExecutor create(int corePoolSize,
                                                 int maximumPoolSize,
                                                 long keepAliveTime,
                                                 TimeUnit unit,
                                                 int queueSize,
                                                 ThreadFactory threadFactory,
                                                 RejectedExecutionHandler handler) {
        EagerTaskQueue<Runnable> workQueue = create(queueSize);
        EagerThreadPoolExecutor executor = new EagerThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        workQueue.updateExecutor(executor);
        return executor;
    }

    protected EagerThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      EagerTaskQueue<Runnable> workQueue) {
        super(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue);
    }

    protected EagerThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      EagerTaskQueue<Runnable> workQueue,
                                      RejectedExecutionHandler handler) {
        super(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                handler);
    }

    protected EagerThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      EagerTaskQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory) {
        super(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory);
    }

    protected EagerThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      EagerTaskQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory,
                                      RejectedExecutionHandler handler) {
        super(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                threadFactory,
                handler);
    }

    /**
     * @return 当前已提交的任务数
     */
    int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        submittedTaskCount.decrementAndGet();
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        //不能通过beforeExecute实现increment, 因为其不能改变当前的行为, 而是改变下一task的执行行为
        submittedTaskCount.incrementAndGet();
        try {
            super.execute(command);
        } catch (RejectedExecutionException rx) {
            //存在可能无法创建新worker, 相当于plan b, 那么就入队
            //尝试入队
            EagerTaskQueue queue = (EagerTaskQueue) super.getQueue();
            try {
                if (!queue.retryOffer(command)) {
                    submittedTaskCount.decrementAndGet();
                    throw new RejectedExecutionException("thread pool executor queue capacity is full.", rx);
                }
            } catch (InterruptedException x) {
                submittedTaskCount.decrementAndGet();
                throw new RejectedExecutionException(x);
            }
        } catch (Throwable t) {
            submittedTaskCount.decrementAndGet();
            throw t;
        }
    }
}
