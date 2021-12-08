package org.kin.framework.utils;

/**
 * 这个实现可以获取一个更好的估算结果, 因为它考虑更多data-point
 *
 * @author huangjianqin
 * @date 2021/12/8
 */
public class MedianEstimator extends QuantileEstimator {
    public MedianEstimator() {
        this.quantile = 0.5;
    }

    @Override
    public synchronized void reset(double quantile) {
        super.reset(0.5);
    }

    public synchronized void reset() {
        reset(0.5);
    }

    @Override
    public synchronized void insert(double x) {
        if (sign == 0) {
            estimate = x;
            sign = 1;
        } else {
            if (x > estimate) {
                greaterThanZero(x);
            } else if (x < estimate) {
                lessThanZero(x);
            }
        }
    }

    private void greaterThanZero(double x) {
        step += sign;

        if (step > 0) {
            estimate += step;
        } else {
            estimate += 1;
        }

        if (estimate > x) {
            step += (x - estimate);
            estimate = x;
        }

        if (sign < 0) {
            step = 1;
        }

        sign = 1;
    }

    private void lessThanZero(double x) {
        step -= sign;

        if (step > 0) {
            estimate -= step;
        } else {
            estimate--;
        }

        if (estimate < x) {
            step += (estimate - x);
            estimate = x;
        }

        if (sign > 0) {
            step = 1;
        }

        sign = -1;
    }

    @Override
    public String toString() {
        return "Median(v=" + estimate + ")";
    }
}
