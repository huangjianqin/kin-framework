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
public class AsyncDBExecutor implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(AsyncDBExecutor.class);
    private final AsyncDBEntity POISON = new AsyncDBEntity() {
    };
    private static final int WAITTING_OPR_NUM_THRESHOLD = 500;
    private static final int LOG_STATE_INTERVAL = 60;


    private ExecutionContext executionContext;
    private AsyncDBOperator[] asyncDBOperators;
    private volatile boolean isStopped = false;
    private AsyncDBStrategy asyncDBStrategy;

    void init(int threadNum, AsyncDBStrategy asyncDBStrategy) {
        Preconditions.checkArgument(threadNum > 0, "thread num must greater than 0");
        executionContext = ExecutionContext.fix(threadNum, "asyncDB");
        this.asyncDBStrategy = asyncDBStrategy;
        asyncDBOperators = new AsyncDBOperator[threadNum];
        for (int i = 0; i < threadNum; i++) {
            AsyncDBOperator asyncDBOperator = new AsyncDBOperator();
            executionContext.execute(asyncDBOperator);
            asyncDBOperators[i] = asyncDBOperator;
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
                for (AsyncDBOperator asyncDBOperator : asyncDBOperators) {
                    SyncState syncState = asyncDBOperator.getSyncState();
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
        for (AsyncDBOperator asyncDBOperator : asyncDBOperators) {
            asyncDBOperator.close();
        }
        executionContext.shutdown();
        try {
            executionContext.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    boolean submit(AsyncDBEntity asyncDBEntity) {
        if (!isStopped) {
            int key = asyncDBEntity.hashCode();
            int index = key % asyncDBOperators.length;
            AsyncDBOperator asyncDBOperator = asyncDBOperators[index];

            asyncDBOperator.submit(asyncDBEntity);
            return true;
        }

        return false;
    }

    private class AsyncDBOperator implements Runnable, Closeable {
        private BlockingQueue<AsyncDBEntity> queue = new LinkedBlockingQueue<>();
        private volatile boolean isStopped = false;
        private long syncNum = 0;
        private String threadName = "";
        private long preSyncNum = 0;

        boolean submit(AsyncDBEntity asyncDBEntity) {
            if (!isStopped) {
                return queue.add(asyncDBEntity);
            }

            return false;
        }

        @Override
        public void run() {
            threadName = Thread.currentThread().getName();
            while (true) {
                int oprNum = asyncDBStrategy.getOprNum();
                for (int i = 0; i < oprNum; i++) {
                    AsyncDBEntity entity = null;
                    try {
                        entity = queue.take();

                        if (entity == POISON) {
                            log.info("AsyncDBOperator return");
                            return;
                        }

                        entity.tryBDOpr(asyncDBStrategy.getTryTimes());

                        syncNum++;
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        if (Objects.nonNull(entity) && DBStatus.UPDATE.equals(entity.getStatus())) {
                            //update操作抛出异常, 重置updating状态
                            entity.resetUpdating();
                        }
                    }
                }

                int duration = asyncDBStrategy.getDuration(queue.size());
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
