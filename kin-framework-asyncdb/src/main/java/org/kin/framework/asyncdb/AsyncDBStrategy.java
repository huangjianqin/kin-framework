package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/4/3
 */
public interface AsyncDBStrategy {
    /**
     * @return 每次处理DB实体的数量
     */
    int getOprNum();

    /**
     * @return DB操作的尝试次数
     */
    int getTryTimes();

    /**
     * @param size 队列大小
     * @return 处理的间隔, 毫秒
     */
    int getDuration(int size);
}
