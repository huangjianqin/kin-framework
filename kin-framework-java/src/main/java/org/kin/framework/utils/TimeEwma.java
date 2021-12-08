package org.kin.framework.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * 基于时间差变化(耗时)的ewm计算工具
 *
 * @author huangjianqin
 * @date 2021/12/8
 */
public class TimeEwma extends AbstractEwma {
    private static final AtomicLongFieldUpdater<TimeEwma> STAMP = AtomicLongFieldUpdater.newUpdater(TimeEwma.class, "stamp");

    private final long tau;
    private volatile long stamp;

    /**
     * @param halfLife     控制β(like most decay process)
     * @param unit         {@code halfLife}时间单位
     * @param initialValue 初始ewma值
     */
    public TimeEwma(long halfLife, TimeUnit unit, double initialValue) {
        //tau约等于1.5*halfLife, 相当于认为多一半的数据量, 计算出来的ewma值相对更新接近真实, 特比是刚开始的时候
        this.tau = TimeUnit.NANOSECONDS.convert((long) (halfLife / Math.log(2)), unit);
        this.ewma = initialValue;
        STAMP.lazySet(this, 0L);
    }

    @Override
    public synchronized void insert(double x) {
        long now = now();
        double elapsed = Math.max(0, now - stamp);

        STAMP.lazySet(this, now);

        //修正β, elapsed越大, β越小
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
