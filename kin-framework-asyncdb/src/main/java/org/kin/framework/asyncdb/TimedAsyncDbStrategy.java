package org.kin.framework.asyncdb;

import com.google.common.base.Preconditions;

/**
 * 基于定时入库策略
 *
 * @author huangjianqin
 * @date 2020/12/27
 */
public class TimedAsyncDbStrategy extends SimpleAsyncDbStrategy {
    public TimedAsyncDbStrategy(int oprNum, int tryTimes, int duration) {
        super(oprNum, tryTimes, duration);
        Preconditions.checkArgument(duration > 0, "duration must be greater than zero");
    }
}
