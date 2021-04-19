package org.kin.framework.concurrent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 当且仅当executor的已提交任务数小于当前线程数 或者 当前线程数>=线程池最大线程池, task才入队
 * 其余情况, 让executor创建new worker去处理task
 *
 * @author huangjianqin
 * @date 2021/4/19
 */
class EagerTaskQueue<R extends Runnable> extends LinkedBlockingQueue<Runnable> {
    private static final long serialVersionUID = -2635853580887179627L;
    /** eager thread pool executor */
    private EagerThreadPoolExecutor executor;

    EagerTaskQueue() {
    }

    EagerTaskQueue(int capacity) {
        super(capacity);
    }

    void updateExecutor(EagerThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean offer(Runnable runnable) {
        if (executor == null) {
            throw new RejectedExecutionException("The task queue does not have executor!");
        }

        //线程池当前线程数量
        int currentPoolThreadSize = executor.getPoolSize();
        //有空闲的worker, 将task push进queue, 让worker处理
        if (executor.getSubmittedTaskCount() < currentPoolThreadSize) {
            return super.offer(runnable);
        }

        //当前线程数量 < 线程池最大线程数
        if (currentPoolThreadSize < executor.getMaximumPoolSize()) {
            return false;
        }

        //当前线程数量 >= 线程池最大线程数, 则入队
        return super.offer(runnable);
    }

    /**
     * 尝试入队
     */
    boolean retryOffer(Runnable o) throws InterruptedException {
        if (executor.isShutdown()) {
            throw new RejectedExecutionException("Executor is shutdown!");
        }
        //尝试入队, 但不阻塞
        return super.offer(o, 0, TimeUnit.MILLISECONDS);
    }
}
