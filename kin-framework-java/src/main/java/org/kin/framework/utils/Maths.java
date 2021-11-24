package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/11/22
 */
public final class Maths {
    private Maths() {
    }

    /**
     * 计算r=log2(M)
     */
    public static double log2(double m) {
        return log(m, 2);
    }

    /**
     * 计算r=logN(M)
     */
    public static double log(double n, double m) {
        return Math.log(m) / Math.log(n);
    }

    /**
     * 判断{@code val}是否是2的N次方
     */
    public static boolean isPower2(int val) {
        return (val & -val) == val;
    }

    /**
     * 最接近的2的n次方值
     */
    public static int round2Power2(int val) {
        if (val > 1073741824) {
            throw new IllegalArgumentException("There is no larger power of 2 int for val:" + val + " since it exceeds 2^31.");
        } else if (val < 0) {
            throw new IllegalArgumentException("Given val:" + val + ". Expecting val >= 0.");
        } else {
            return 1 << 32 - Integer.numberOfLeadingZeros(val - 1);
        }
    }
}
