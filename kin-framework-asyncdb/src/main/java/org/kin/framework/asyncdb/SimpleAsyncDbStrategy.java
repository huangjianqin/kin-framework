package org.kin.framework.asyncdb;

/**
 * 简单实现
 *
 * @author huangjianqin
 * @date 2020/12/27
 */
public class SimpleAsyncDbStrategy implements AsyncDbStrategy {
    /** 每个worker每轮处理DB实体的数量 */
    private final int oprNum;
    /** DB操作的重试次数 */
    private final int tryTimes;
    /** 处理的间隔, 毫秒 */
    private final int duration;

    public SimpleAsyncDbStrategy(int oprNum, int tryTimes, int duration) {
        this.oprNum = oprNum;
        this.tryTimes = tryTimes;
        this.duration = duration;
    }

    @Override
    public final int getOprNum() {
        return oprNum;
    }

    @Override
    public final int getRetryTimes() {
        return tryTimes;
    }

    @Override
    public final int getDuration(int size) {
        return duration;
    }
}
