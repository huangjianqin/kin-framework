package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/3/31
 */
enum DbStatus {
    /**
     * 正常状态
     */
    NORMAL {
        @Override
        public boolean execute(DbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            return true;
        }
    },
    /**
     * DB记录正在插入状态
     */
    INSERT {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execute(DbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            dbSynchronzier.insert(asyncDbEntity);
            return true;
        }
    },
    /**
     * DB记录正在更新状态
     */
    UPDATE {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execute(DbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            dbSynchronzier.update(asyncDbEntity);
            return true;
        }
    },
    /**
     * DB记录正在删除状态
     */
    DELETED {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execute(DbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            dbSynchronzier.delete(asyncDbEntity);
            return true;
        }
    },
    ;

    /**
     * 执行db操作
     */
    public abstract boolean execute(DbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity);
}
