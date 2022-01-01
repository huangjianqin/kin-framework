package org.kin.framework.utils;

/**
 * 指数移动加权平均(Exponentially Weighted Moving Average)较传统的平均法来说, 一是不需要保存过去所有的数值；二是计算量显著减小.
 * vt=βvt−1+(1−β)θt
 * vt即t时的ewma值, 则vt−1表示t-1时的ewma值
 * θt即t时反应的真实值(不同使用场景, 表示的值不太一样)
 * β表示加权下降的速率, 其值越小下降的越快
 *
 * @author huangjianqin
 * @date 2021/12/8
 */
public abstract class AbstractEwma {
    protected volatile double ewma;

    /**
     * 计算下一时刻的ewma值
     *
     * @param x θt
     */
    public abstract void insert(double x);

    /**
     * 计算下一时刻的ewma值
     *
     * @param w β
     * @param x θt
     */
    protected void insert(double w, double x) {
        ewma = w * ewma + (1.0 - w) * x;
    }

    /**
     * 重置ewma值
     */
    protected void reset(double value) {
        ewma = value;
    }

    //getter
    public double getEwma() {
        return ewma;
    }
}
