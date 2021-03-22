package org.kin.framework.asyncdb;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author huangjianqin
 * @date 2019/3/31
 * 支持多线程操作
 */
public abstract class AsyncDbEntity<PK extends Serializable> implements Serializable {
    private static final long serialVersionUID = -3533132945914928362L;
    /** 数据库状态 */
    private final AtomicReference<DbStatus> status = new AtomicReference<>(DbStatus.NORMAL);
    /** 数据库同步操作 */
    private volatile DbSynchronzier<PK, ?> dbSynchronzier;
    /** 是否支持删除操作 */
    private final boolean canDelete;

    public AsyncDbEntity() {
        this(false);
    }

    public AsyncDbEntity(boolean canDelete) {
        this.canDelete = canDelete;
    }

    /**
     * @return 主键
     */
    public abstract PK getPrimaryKey();

    /**
     * entity insert
     */
    public final void insert() {
        AsyncDbService.getInstance().dbOpr(this, DbOperation.INSERT);
    }

    /**
     * entity update
     */
    public final void update() {
        AsyncDbService.getInstance().dbOpr(this, DbOperation.UPDATE);
    }

    /**
     * entity delete
     */
    public final void delete() {
        if (!canDelete) {
            return;
        }
        AsyncDbService.getInstance().dbOpr(this, DbOperation.DELETE);
    }

    protected void serialize() {
        //do nothing, waitting to overwrite
    }

    protected void deserialize() {
        //do nothing, waitting to overwrite
    }

    /**
     * 当前db status是否允许执行指定DB操作
     */
    boolean isCanPersist(DbOperation operation) {
        DbStatus now;
        do {
            now = getStatus();
            if (!operation.isCanTransfer(now)) {
                throw new AsyncDbException("DB操作失败 -> " + toString() + " - " + now + " - " + operation);
            }
        } while (!status.compareAndSet(now, operation.getTargetStauts()));
        return now == DbStatus.NORMAL;
    }

    /**
     * 切换db status, 并真正尝试执行db操作
     */
    boolean tryDbOpr(int retryTimes) {
        DbStatus now;
        do {
            now = getStatus();
        } while (!status.compareAndSet(now, now == DbStatus.DELETED ? DbStatus.DELETED : DbStatus.NORMAL));


        int nowTry = 0;
        do {
            if (nowTry++ > retryTimes) {
                return false;
            }
        } while (!now.execute(dbSynchronzier, this));

        return true;
    }

    //setter && getter
    DbSynchronzier<PK, ?> getDbSynchronzier() {
        return dbSynchronzier;
    }

    void setDbSynchronzier(DbSynchronzier<PK, ?> dbSynchronzier) {
        this.dbSynchronzier = dbSynchronzier;
    }

    DbStatus getStatus() {
        return status.get();
    }

    public boolean isCanDelete() {
        return canDelete;
    }
}
