package org.kin.framework.asyncdb;

import com.google.common.base.Preconditions;
import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/4/1
 */
public class AsyncDbExecutor implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(AsyncDbExecutor.class);
    /** 终止executor的db实体 */
    private final AsyncDbEntity POISON = new AsyncDbEntity() {
    };
    /** 等待db实体操作数量阈值, 超过这个阈值, 会打印日志 */
    private static final int WAITTING_OPR_NUM_THRESHOLD = 500;
    /** 打印executor状态信息log间隔 */
    private static final int LOG_STATE_INTERVAL = 60;
    /** 线程池 */
    private ExecutionContext executionContext;
    /** executor数量 */
    private AsyncDBOperator[] asyncDbOperators;
    /** executor终止标识 */
    private volatile boolean isStopped = false;
    /** executor执行db操作策略 */
    private AsyncDbStrategy asyncDbStrategy;

    void init(int threadNum, AsyncDbStrategy asyncDbStrategy) {
        Preconditions.checkArgument(threadNum > 0, "thread num must greater than 0");
        executionContext = ExecutionContext.fix(threadNum, "asyncDB");
        this.asyncDbStrategy = asyncDbStrategy;
        asyncDbOperators = new AsyncDBOperator[threadNum];
        for (int i = 0; i < threadNum; i++) {
            AsyncDBOperator asyncDbOperator = new AsyncDBOperator();
            executionContext.execute(asyncDbOperator);
            asyncDbOperators[i] = asyncDbOperator;
        }
        executionContext.execute(() -> {
            while (!isStopped) {
                long sleepTime = LOG_STATE_INTERVAL - TimeUtils.timestamp() % LOG_STATE_INTERVAL;
                try {
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {

                }

                int totalTaskOpredNum = 0;
                int totalWaittingOprNum = 0;
                for (AsyncDBOperator asyncDbOperator : asyncDbOperators) {
                    SyncState syncState = asyncDbOperator.getSyncState();
                    log.info("{} -> taskOpredNum: {}, taittingOprNum: {}, taskOpredPeriodNum: {}",
                            syncState.getThreadName(), syncState.getSyncNum(), syncState.getWaittingOprNum(),
                            syncState.getSyncPeriodNum());
                    totalTaskOpredNum += syncState.getSyncNum();
                    totalWaittingOprNum += syncState.getWaittingOprNum();
                }
                if (totalWaittingOprNum > WAITTING_OPR_NUM_THRESHOLD) {
                    log.warn("totalTaskOpredNum: {}, totalWaittingOprNum: {}", totalTaskOpredNum, totalWaittingOprNum);
                } else {
                    log.info("totalTaskOpredNum: {}, totalWaittingOprNum: {}", totalTaskOpredNum, totalWaittingOprNum);
                }
            }
        });
    }

    @Override
    public void close() {
        isStopped = true;
        for (AsyncDBOperator asyncDbOperator : asyncDbOperators) {
            asyncDbOperator.close();
        }
        executionContext.shutdown();
        try {
            executionContext.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    boolean submit(AsyncDbEntity asyncDbEntity) {
        if (!isStopped) {
            int key = asyncDbEntity.hashCode();
            int index = key % asyncDbOperators.length;
            AsyncDBOperator asyncDBOperator = asyncDbOperators[index];

            asyncDBOperator.submit(asyncDbEntity);
            return true;
        }

        return false;
    }

    private class AsyncDBOperator implements Runnable, Closeable {
        private BlockingQueue<AsyncDbEntity> queue = new LinkedBlockingQueue<>();
        private volatile boolean isStopped = false;
        private long syncNum = 0;
        private String threadName = "";
        private long preSyncNum = 0;

        boolean submit(AsyncDbEntity asyncDbEntity) {
            if (!isStopped) {
                return queue.add(asyncDbEntity);
            }

            return false;
        }

        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            while (true) {
                int oprNum = asyncDbStrategy.getOprNum();
                for (int i = 0; i < oprNum; i++) {
                    AsyncDbEntity entity = null;
                    try {
                        entity = queue.take();

                        if (entity == POISON) {
                            log.info("AsyncDBOperator return");
                            return;
                        }

                        entity.tryDbOpr(asyncDbStrategy.getTryTimes());

                        syncNum++;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        if (Objects.nonNull(entity) && DbStatus.UPDATE.equals(entity.getStatus())) {
                            //update操作抛出异常, 重置updating状态
                            entity.resetUpdating();
                        }
                    }
                }

                int duration = asyncDbStrategy.getDuration(queue.size());
                if (!isStopped) {
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    if (queue.isEmpty()) {
                        return;
                    }
                }
            }
        }

        @Override
        public void close() {
            submit(POISON);
            isStopped = true;
        }

        SyncState getSyncState() {
            long syncNum = this.syncNum;
            long preSyncNum = this.preSyncNum;
            int waittingOprNum = queue.size();
            long syncNumPeriodNum = syncNum - preSyncNum;
            this.preSyncNum = syncNum;

            return new SyncState(threadName, syncNum, waittingOprNum, syncNumPeriodNum);
        }
    }
}
