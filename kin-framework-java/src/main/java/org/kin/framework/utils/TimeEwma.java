package org.kin.framework.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * 基于时间差(耗时)β变化的ewma预测工具
 *
 * @author huangjianqin
 * @date 2021/12/8
 */
public class TimeEwma extends AbstractEwma {
    private static final AtomicLongFieldUpdater<TimeEwma> STAMP = AtomicLongFieldUpdater.newUpdater(TimeEwma.class, "stamp");

    /** (希腊字母)为EWMA的时间常量 */
    private final long tau;
    private volatile long stamp;

    /**
     * @param halfLife     speed of convergence，即收敛的速度, 控制β(like most decay process)
     * @param unit         {@code halfLife}时间单位
     * @param initialValue 初始ewma值
     */
    public TimeEwma(long halfLife, TimeUnit unit, double initialValue) {
        //tau约等于1.5*halfLife, 相当于认为多一半的数据量, 计算出来的ewma值相对更新接近真实, 特别是刚开始的时候
        this.tau = TimeUnit.NANOSECONDS.convert((long) (halfLife / Math.log(2)), unit);
        this.ewma = initialValue;
        STAMP.lazySet(this, 0L);
    }

    @Override
    public synchronized void insert(double x) {
        long now = now();
        //距离上次计算逝去的时间
        double elapsed = Math.max(0, now - stamp);

        STAMP.lazySet(this, now);

        //修正β, elapsed越大, β越小
        //可以认为时间过得越长, 当前越不靠谱, 所以下降得更慢些
        double w = Math.exp(-elapsed / tau);
        super.insert(w, x);
    }

    @Override
    public synchronized void reset(double value) {
        stamp = 0L;
        super.reset(value);
    }

    private long now() {
        return System.nanoTime() / 1000;
    }

    @Override
    public String toString() {
        return "TimeEwma(value=" + ewma + ", age=" + (now() - stamp) + ")";
    }
}
