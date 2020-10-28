package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class GetExceptionDescTest {
    public static void main(String[] args) {
        try {
            int a = 1 / 0;
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getExceptionDesc(e));
        }
    }
}
