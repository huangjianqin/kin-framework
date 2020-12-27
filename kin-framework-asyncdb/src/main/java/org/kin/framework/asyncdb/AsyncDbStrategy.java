package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/4/3
 */
interface AsyncDbStrategy {
    /**
     * @return 每个worker每轮处理DB实体的数量
     */
    int getOprNum();

    /**
     * @return DB操作的重试次数
     */
    int getRetryTimes();

    /**
     * @param size 当前等待队列大小
     * @return 处理的间隔, 毫秒
     */
    int getDuration(int size);
}
