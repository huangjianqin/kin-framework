package org.kin.framework.math;

/**
 * @author huangjianqin
 * @date 2020-01-12
 */
public class IntCounter {
    private int number = 0;

    public IntCounter() {
    }

    public IntCounter(int number) {
        this.number = number;
    }

    public void increment() {
        number++;
    }

    public int getCount() {
        return number;
    }
}
