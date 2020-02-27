package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2018/2/26
 */
public class SysUtils {
    public static final int CPU_NUM = Runtime.getRuntime().availableProcessors();

    public static int getSuitableThreadNum() {
        return CPU_NUM * 2 - 1;
    }
}
