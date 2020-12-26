package org.kin.framework.asyncdb;

/**
 * 无延迟入库, worker不停顿, 不停consume entity
 *
 * @author huangjianqin
 * @date 2019/4/3
 */
public class NoDelayAsyncDbStrategy extends SimpleAsyncDbStrategy {
    public NoDelayAsyncDbStrategy(int oprNum, int tryTimes) {
        super(oprNum, tryTimes, 0);
    }
}
