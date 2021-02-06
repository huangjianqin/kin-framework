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
    INSERT(DbStatus.INSERT, Arrays.asList(DbStatus.NORMAL, DbStatus.DELETED)),
    /**
     * DB记录更新
     */
    UPDATE(DbStatus.UPDATE, Arrays.asList(DbStatus.INSERT, DbStatus.UPDATE)),
    /**
     * DB记录删除
     */
    DELETE(DbStatus.DELETED, Arrays.asList(DbStatus.INSERT, DbStatus.UPDATE)),
    ;
    /**
     * DB状态
     */
    private final DbStatus targetStauts;
    /**
     * 可以切换成本状态的DB状态
     */
    private final List<DbStatus> canTransfer;

    /** db operation缓存 */
    private static final DbOperation[] DB_OPERATIONS = values();

    DbOperation(DbStatus targetStauts, List<DbStatus> canTransfer) {
        this.targetStauts = targetStauts;
        this.canTransfer = canTransfer;
    }

    DbStatus getTargetStauts() {
        return targetStauts;
    }

    /**
     * @return status是否可以切换到targetStauts
     */
    boolean isCanTransfer(DbStatus status) {
        return canTransfer.contains(status);
    }

    /**
     * 根据targetStauts获取db operation
     */
    public static DbOperation getByTargetStauts(DbStatus dbStatus) {
        for (DbOperation dbOperation : DB_OPERATIONS) {
            if (dbOperation.getTargetStauts().equals(dbStatus)) {
                return dbOperation;
            }
        }

        throw new IllegalArgumentException("找不到对应db operation");
    }
}
