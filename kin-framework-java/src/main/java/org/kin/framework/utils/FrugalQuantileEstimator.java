package org.kin.framework.utils;

import java.util.SplittableRandom;

/**
 * Reference: Ma, Qiang, S. Muthukrishnan, and Mark Sandler. "Frugal Streaming for Estimating
 * Quantiles." Space-Efficient Data Structures, Streams, and Algorithms. Springer Berlin Heidelberg,
 * 2013. 77-96.
 * <p>More info: http://blog.aggregateknowledge.com/2013/09/16/sketch-of-the-day-frugal-streaming/
 *
 * @author huangjianqin
 * @date 2021/12/8
 */
public class FrugalQuantileEstimator extends QuantileEstimator {
    private final double increment;
    private final SplittableRandom rnd;

    public FrugalQuantileEstimator(double quantile) {
        this(quantile, 1.0);
    }

    public FrugalQuantileEstimator(double quantile, double increment) {
        this.increment = increment;
        this.quantile = quantile;
        this.estimate = 0.0;
        this.step = 1;
        this.sign = 0;
        this.rnd = new SplittableRandom(System.nanoTime());
    }

    @Override
    public double estimation() {
        return estimate;
    }

    @Override
    public synchronized void insert(double x) {
        if (sign == 0) {
            estimate = x;
            sign = 1;
        } else {
            double v = rnd.nextDouble();

            if (x > estimate && v > (1 - quantile)) {
                higher(x);
            } else if (x < estimate && v > quantile) {
                lower(x);
            }
        }
    }

    private void higher(double x) {
        step += sign * increment;

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

    private void lower(double x) {
        step -= sign * increment;

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
        return "FrugalQuantile(q=" + quantile + ", v=" + estimate + ")";
    }
}
