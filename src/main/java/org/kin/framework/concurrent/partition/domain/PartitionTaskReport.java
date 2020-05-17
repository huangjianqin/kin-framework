package org.kin.framework.concurrent.partition.domain;

/**
 * @author huangjianqin
 * @date 2019/6/3
 */
public class PartitionTaskReport {
    private String threadName;
    private long pendingTaskNum;
    private long finishedTaskNum;

    public PartitionTaskReport(String threadName, long pendingTaskNum, long finishedTaskNum) {
        this.threadName = threadName;
        this.pendingTaskNum = pendingTaskNum;
        this.finishedTaskNum = finishedTaskNum;
    }

    //getter

    public String getThreadName() {
        return threadName;
    }

    public long getPendingTaskNum() {
        return pendingTaskNum;
    }

    public long getFinishedTaskNum() {
        return finishedTaskNum;
    }
}
