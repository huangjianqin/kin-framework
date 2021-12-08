package org.kin.framework.utils;

/**
 * 分位数值估算
 * 二分, 取中间的值
 *
 * @author huangjianqin
 * @date 2021/12/8
 */
public abstract class QuantileEstimator {
    /** n分, 比如2分, 则n=0.5, 可以认为估算位于0.5左右的数值, 如果是0.75, 则是估算位于0.75左右的数值 */
    protected double quantile;
    /** 估算值 */
    protected volatile double estimate;
    protected int step;
    protected int sign;

    public QuantileEstimator() {
        reset(0.0);
    }

    /**
     * 返回估算值
     */
    public double estimation() {
        return estimate;
    }

    /**
     * Insert a data point `x` in the quantile estimator.
     *
     * @param x the data point to add.
     */
    public abstract void insert(double x);

    /**
     * 重置
     */
    public synchronized void reset(double quantile) {
        this.quantile = quantile;
        this.estimate = 0.0;
        this.step = 1;
        this.sign = 0;
    }
}
