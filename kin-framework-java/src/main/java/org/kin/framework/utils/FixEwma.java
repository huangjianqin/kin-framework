package org.kin.framework.utils;

/**
 * 固定β的ewma预测工具
 *
 * @author huangjianqin
 * @date 2021/12/16
 */
public class FixEwma extends AbstractEwma {
    private final double w;

    public FixEwma(double w) {
        this.w = w;
    }

    @Override
    public synchronized void insert(double x) {
        super.insert(w, x);
    }
}
