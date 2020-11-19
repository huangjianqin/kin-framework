package org.kin.framework.asyncdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author huangjianqin
 * @date 2019/3/31
 * 支持多线程操作
 */
public abstract class AsyncDBEntity implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(AsyncDBEntity.class);
    /** 数据库状态 */
    private volatile AtomicReference<DBStatus> status = new AtomicReference<>(DBStatus.NORMAL);
    /** 数据库同步操作 */
    private volatile DBSynchronzier DBSynchronzier;
    /** 实体进行过update标识, 主要用于减少update的次数 */
    private AtomicBoolean updating = new AtomicBoolean();

    public void insert() {
        AsyncDBService.getInstance().dbOpr(this, DBOperation.Insert);
    }

    public void update() {
        AsyncDBService.getInstance().dbOpr(this, DBOperation.Update);
    }

    public void delete() {
        AsyncDBService.getInstance().dbOpr(this, DBOperation.Delete);
    }

    protected void serialize() {
        //do nothing, waitting to overwrite
    }

    protected void deserialize() {
        //do nothing, waitting to overwrite
    }

    boolean isCanPersist(DBOperation operation) {
        DBStatus now;
        do {
            now = getStatus();
            if (!operation.isCanTransfer(now)) {
                throw new AsyncDBException("DB操作失败 -> " + toString() + " - " + now + " - " + operation);
            }
        } while (!status.compareAndSet(now, operation.getTargetStauts()));
        return now == DBStatus.NORMAL;
    }


    boolean tryBDOpr(int tryTimes) {
        DBStatus now;
        do {
            now = getStatus();
        } while (!status.compareAndSet(now, now == DBStatus.DELETED ? DBStatus.DELETED : DBStatus.NORMAL));


        int nowTry = 0;
        do {
            if (nowTry++ >= tryTimes) {
                return false;

            }
        } while (!now.execute(DBSynchronzier, this));

        return true;
    }

    /**
     * 尝试获取进行更新, 如果前一次更新还未进行完, 本次则skip
     */
    boolean tryUpdate() {
        return updating.compareAndSet(false, true);
    }

    /**
     * 重置实体更新中标识
     */
    void resetUpdating() {
        if (!updating.compareAndSet(true, false)) {
            //异常, 直接set
            updating.set(false);
        }
    }

    //setter && getter
    DBSynchronzier getAsyncPersistent() {
        return DBSynchronzier;
    }

    void setAsyncPersistent(DBSynchronzier DBSynchronzier) {
        this.DBSynchronzier = DBSynchronzier;
    }

    DBStatus getStatus() {
        return status.get();
    }
}
