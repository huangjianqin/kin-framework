package org.kin.framework.asyncdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2019/3/31
 */
public enum DbStatus {
    /**
     * 正常状态
     */
    NORMAL {
        @Override
        public boolean execute(AbstractDbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            return true;
        }
    },
    /**
     * DB记录正在插入状态
     */
    INSERT {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execute(AbstractDbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            try {
                dbSynchronzier.insert(asyncDbEntity);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return false;
        }
    },
    /**
     * DB记录正在更新状态
     */
    UPDATE {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execute(AbstractDbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            try {
                //重置实体更新中标识
                asyncDbEntity.resetUpdating();
                dbSynchronzier.update(asyncDbEntity);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return false;
        }
    },
    /**
     * DB记录正在删除状态
     */
    DELETED {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execute(AbstractDbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity) {
            try {
                dbSynchronzier.delete(asyncDbEntity);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return false;
        }
    },
    ;

    private static final Logger log = LoggerFactory.getLogger(DbStatus.class);

    /**
     * 执行db操作
     */
    public abstract boolean execute(AbstractDbSynchronzier dbSynchronzier, AsyncDbEntity asyncDbEntity);
}
