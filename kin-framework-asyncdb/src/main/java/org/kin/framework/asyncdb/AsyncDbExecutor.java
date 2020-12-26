package org.kin.framework.asyncdb;

import com.google.common.base.Preconditions;
import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.TimeUtils;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/4/1
 */
public class AsyncDbExecutor implements Closeable, LoggerOprs {
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
        //定时打印所有executor信息状态
        executionContext.execute(() -> {
            while (!isStopped) {
                long sleepTime = LOG_STATE_INTERVAL - TimeUtils.timestamp() % LOG_STATE_INTERVAL;
                try {
                    TimeUnit.SECONDS.sleep(sleepTime);
                } catch (InterruptedException e) {
                    //ignore
                }

                int totalTaskOpredNum = 0;
                int totalWaittingOprNum = 0;
                for (AsyncDBOperator asyncDbOperator : asyncDbOperators) {
                    SyncState syncState = asyncDbOperator.getSyncState();
                    info("{} -> taskOpredNum: {}, taittingOprNum: {}, taskOpredPeriodNum: {}",
                            syncState.getThreadName(), syncState.getSyncNum(), syncState.getWaittingOprNum(),
                            syncState.getSyncPeriodNum());
                    totalTaskOpredNum += syncState.getSyncNum();
                    totalWaittingOprNum += syncState.getWaittingOprNum();
                }
                if (totalWaittingOprNum > WAITTING_OPR_NUM_THRESHOLD) {
                    warn("totalTaskOpredNum: {}, totalWaittingOprNum: {}", totalTaskOpredNum, totalWaittingOprNum);
                } else {
                    info("totalTaskOpredNum: {}, totalWaittingOprNum: {}", totalTaskOpredNum, totalWaittingOprNum);
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
            error(e.getMessage(), e);
        }
    }

    /**
     * 提交db操作task
     */
    boolean submit(AsyncDbEntity asyncDbEntity) {
        if (!isStopped) {
            int key = asyncDbEntity.hashCode();
            int index = key % asyncDbOperators.length;
            AsyncDBOperator asyncDbOperator = asyncDbOperators[index];

            asyncDbOperator.submit(asyncDbEntity);
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

        /**
         * 入队
         */
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
                    } catch (InterruptedException e) {
                        //ignore
                        //todo 放到监听器去实现
//                        if (Objects.nonNull(entity) && DbStatus.UPDATE.equals(entity.getStatus())) {
//                            //update操作抛出异常, 重置updating状态
//                            entity.resetUpdating();
//                        }
                    }

                    if (Objects.isNull(entity)) {
                        continue;
                    }

                    if (entity == POISON) {
                        info("AsyncDBOperator return");
                        return;
                    }

                    entity.tryDbOpr(asyncDbStrategy.getTryTimes());

                    syncNum++;
                }

                int duration = asyncDbStrategy.getDuration(queue.size());
                if (!isStopped) {
                    if (duration > 0) {
                        try {
                            Thread.sleep(duration);
                        } catch (InterruptedException e) {
                            //ignore
                        }
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

        /**
         * 获取该executor信息状态
         */
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
