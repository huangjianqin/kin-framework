package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/11/22
 */
public final class Maths {
    private Maths() {
    }

    /**
     * 计算r=log2(n)
     */
    public static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    /**
     * 判断{@code val}是否是2的N次方
     */
    public static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }
}
