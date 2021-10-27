package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/10/27
 */
public class TimeUtilsTest {
    public static void main(String[] args) {
        System.out.println(TimeUtils.formatDateTime());
        System.out.println(TimeUtils.formatDate());
        System.out.println(TimeUtils.parseDateTime("2021-10-27 20:19:39"));
    }
}
