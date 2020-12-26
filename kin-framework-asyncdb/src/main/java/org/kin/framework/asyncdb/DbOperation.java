package org.kin.framework.asyncdb;

import java.util.Arrays;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2019/4/1
 */
public enum DbOperation {
    /**
     * DB记录插入
     */
    Insert(DbStatus.INSERT, Arrays.asList(DbStatus.NORMAL, DbStatus.DELETED)),
    /**
     * DB记录更新
     */
    Update(DbStatus.UPDATE, Arrays.asList(DbStatus.INSERT, DbStatus.UPDATE)),
    /**
     * DB记录删除
     */
    Delete(DbStatus.DELETED, Arrays.asList(DbStatus.INSERT, DbStatus.UPDATE)),
    ;
    /**
     * DB状态
     */
    private final DbStatus targetStauts;
    /**
     * 可以切换成本状态的DB状态
     */
    private final List<DbStatus> canTransfer;

    DbOperation(DbStatus targetStauts, List<DbStatus> canTransfer) {
        this.targetStauts = targetStauts;
        this.canTransfer = canTransfer;
    }

    DbStatus getTargetStauts() {
        return targetStauts;
    }

    boolean isCanTransfer(DbStatus status) {
        return canTransfer.contains(status);
    }
}
