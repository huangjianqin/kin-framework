package org.kin.framework.concurrent.partition;

import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.concurrent.partition.domain.PartitionTaskReport;
import org.kin.framework.concurrent.partition.impl.HashPartitioner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2017/10/26
 * 利用Task的某种属性将task分区,从而达到同一类的task按submit/execute顺序在同一线程执行
 */
public class PartitionTaskExecutor<K> {
    private static final Logger log = LoggerFactory.getLogger(PartitionTaskExecutor.class);
    private static final int REPORT_INTERVAL = 30;
    private static final int BATCH_MAX_NUM = 100;

    /** 分区数 */
    private final int partitionNum;
    /** 分区算法 */
    private final Partitioner<K> partitioner;
    /** 执行线程池 */
    private final ExecutionContext workers;
    /**
     * 所有分区执行线程实例
     * lazy init
     */
    private PartitionWorker[] partitionWorkers;
    /** report 线程 */
    private final ExecutionContext reportThread;
    /** 每批处理任务最大数 */
    private final int batchMaxNum;

    public PartitionTaskExecutor() {
        this(5);
    }

    public PartitionTaskExecutor(int partitionNum) {
        this(partitionNum, HashPartitioner.INSTANCE);
    }

    public PartitionTaskExecutor(int partitionNum, Partitioner<K> partitioner) {
        this(partitionNum, partitioner, BATCH_MAX_NUM);
    }

    public PartitionTaskExecutor(int partitionNum, Partitioner<K> partitioner, String workerNamePrefix) {
        this(partitionNum, partitioner, BATCH_MAX_NUM, workerNamePrefix);
    }

    public PartitionTaskExecutor(int partitionNum, Partitioner<K> partitioner, int batchMaxNum) {
        this(partitionNum, partitioner, batchMaxNum, "partition-task-executor-");
    }

    public PartitionTaskExecutor(int partitionNum, Partitioner<K> partitioner, int batchMaxNum, String workerNamePrefix) {
        Preconditions.checkArgument(partitionNum > 0, "partitionNum field must be greater then 0");
        Preconditions.checkArgument(batchMaxNum > 0, "batchMaxNum field must be greater then 0");

        this.partitionNum = partitionNum;
        this.partitioner = partitioner;
        this.workers = ExecutionContext.fix(partitionNum, workerNamePrefix);
        this.reportThread = ExecutionContext.fix(1, workerNamePrefix.concat("reporter-"));
        this.batchMaxNum = batchMaxNum;
        this.partitionWorkers = new PartitionTaskExecutor.PartitionWorker[this.partitionNum];

        init();
    }

    //------------------------------------------------------------------------------------------------------------------
    private void init() {
        for (int i = 0; i < partitionNum; i++) {
            PartitionWorker partitionWorker = new PartitionWorker();
            partitionWorkers[i] = partitionWorker;
        }
    }

    private void report() {
        reportThread.execute(() -> {
            while (CollectionUtils.isNonEmpty(partitionWorkers)) {
                long sleepTime = REPORT_INTERVAL - TimeUtils.timestamp() % REPORT_INTERVAL;
                try {
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //do nothing
                }

                report0();
            }
        });
    }

    private void report0() {
        if (partitionWorkers == null) {
            return;
        }
        PartitionWorker[] copy = partitionWorkers;
        List<PartitionTaskReport> reports = new ArrayList<>(copy.length);
        for (PartitionWorker partitionWorker : copy) {
            if (partitionWorker != null) {
                PartitionTaskReport report = partitionWorker.report();
                if (Objects.nonNull(report)) {
                    reports.add(report);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("taskNum: ").append(reports.size()).append(" >>>").append(System.lineSeparator());
        sb.append("threadName\t").append("pendingTaskNum\t").append("finishedTaskNum").append(System.lineSeparator());
        for (PartitionTaskReport report : reports) {
            sb.append(report.getThreadName()).append("\t");
            sb.append(report.getPendingTaskNum()).append("\t");
            sb.append(report.getFinishedTaskNum()).append(System.lineSeparator());
        }

        log.info(sb.toString());
    }

    //------------------------------------------------------------------------------------------------------------------

    public Future<?> execute(K key, Runnable task) {
        return execute(key, task, null);
    }

    public <T> Future<T> execute(K key, Runnable task, T value) {
        return execute(key, Executors.callable(task, value));
    }

    public <T> Future<T> execute(K key, Callable<T> task) {
        PartitionWorker partitionWorker = partitionWorkers[partitioner.toPartition(key, partitionNum)];
        if (partitionWorker != null) {
            FutureTask<T> futureTask = new FutureTask<>(task);

            partitionWorker.execute(new Task(key, futureTask));

            //lazy run
            if (!partitionWorker.isRunning()) {
                synchronized (partitionWorker) {
                    if (!partitionWorker.isRunning()) {
                        workers.execute(partitionWorker);
                    }
                }
            }

            return futureTask;
        } else {
            return null;
        }
    }

    private void shutdown0() {
        if (partitionWorkers == null) {
            return;
        }
        //先关闭执行线程实例再关闭线程池
        //关闭并移除分区执行线程实例,且缓存
        for (PartitionWorker task : partitionWorkers) {
            if (task != null) {
                task.close();
            }
        }
        workers.shutdown();
        reportThread.shutdown();
    }

    public void shutdown() {
        shutdown0();
    }

    public void shutdownNow() {
        shutdown0();
    }

    //------------------------------------------------------------------------------------------------------------------
    private class Task implements Runnable {
        //缓存分区key,以便重分区时获取分区key
        private final K key;
        private final Runnable target;

        Task(K key, Runnable target) {
            this.key = key;
            this.target = target;
        }

        @Override
        public void run() {
            target.run();
        }
    }

    /**
     * task 执行
     */
    private class PartitionWorker implements Runnable {
        //任务队列
        private BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
        //绑定的线程
        private volatile Thread bind;

        private volatile boolean isStopped = false;
        private volatile boolean isTerminated = false;

        private long finishedTaskNum;

        void execute(Task task) {
            try {
                queue.put(task);
            } catch (InterruptedException e) {
                //do nothing
            }
        }

        void close() {
            isStopped = true;
            if (bind != null) {
                bind.interrupt();
            }
        }

        /**
         * @param limit 处理任务上限, -1无上限
         */
        private void run0(Task one, int limit) {
            Collection<Task> waittingTasks = new ArrayList<>(limit + 1);
            if (Objects.nonNull(one)) {
                queue.add(one);
            }
            queue.drainTo(waittingTasks, limit);

            for (Task waittingTask : waittingTasks) {
                try {
                    waittingTask.run();
                    finishedTaskNum++;
                } catch (Exception e) {
                    ExceptionUtils.log(e);
                }
            }
        }

        @Override
        public void run() {
            bind = Thread.currentThread();
            try {
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    try {
                        Task task = queue.take();
                        run0(task, batchMaxNum - 1);
                    } catch (InterruptedException e) {
                        //do nothing
                    }
                }
            } finally {
                run0(null, Integer.MAX_VALUE);
            }
            isTerminated = true;
        }

        PartitionTaskReport report() {
            if (Objects.nonNull(bind)) {
                return new PartitionTaskReport(bind.getName(), queue.size(), finishedTaskNum);
            }

            return null;
        }

        boolean isTerminated() {
            return isTerminated;
        }

        boolean isRunning() {
            return Objects.nonNull(bind);
        }
    }
}
