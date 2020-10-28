package org.kin.framework.asyncdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2019/3/31
 */
public enum DBStatus {
    /**
     * 正常状态
     */
    NORMAL {
        @Override
        public boolean execute(DBSynchronzier DBSynchronzier, AsyncDBEntity asyncDBEntity) {
            return true;
        }
    },
    /**
     * DB记录正在插入状态
     */
    INSERT {
        @Override
        public boolean execute(DBSynchronzier DBSynchronzier, AsyncDBEntity asyncDBEntity) {
            try {
                DBSynchronzier.insert(asyncDBEntity);
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
        @Override
        public boolean execute(DBSynchronzier DBSynchronzier, AsyncDBEntity asyncDBEntity) {
            try {
                DBSynchronzier.update(asyncDBEntity);
                //重置实体更新中标识
                asyncDBEntity.resetUpdate();
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
        @Override
        public boolean execute(DBSynchronzier DBSynchronzier, AsyncDBEntity asyncDBEntity) {
            try {
                DBSynchronzier.delete(asyncDBEntity);
                return true;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            return false;
        }
    },
    ;

    private static final Logger log = LoggerFactory.getLogger(DBStatus.class);

    public abstract boolean execute(DBSynchronzier DBSynchronzier, AsyncDBEntity asyncDBEntity);

}
