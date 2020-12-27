package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/4/4
 */
class SyncState {
    /** 线程名 */
    private final String threadName;
    /** 当前处理的DB 实体数量 */
    private final long syncNum;
    /** 等待db操作的实体数量 */
    private final int waittingOprNum;
    /** 离上次记录期间处理的DB 实体数量 */
    private final long syncPeriodNum;

    public SyncState(String threadName, long syncNum, int waittingOprNum, long syncPeriodNum) {
        this.threadName = threadName;
        this.syncNum = syncNum;
        this.waittingOprNum = waittingOprNum;
        this.syncPeriodNum = syncPeriodNum;
    }

    //setter && getter
    public String getThreadName() {
        return threadName;
    }

    public long getSyncNum() {
        return syncNum;
    }

    public int getWaittingOprNum() {
        return waittingOprNum;
    }

    public long getSyncPeriodNum() {
        return syncPeriodNum;
    }
}
