package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2019-12-12
 */
public class IPUtilsTest {
    public static void main(String[] args) {
        System.out.println(NetUtils.getIp());
        //检查端口是否被占用
        System.out.println(NetUtils.isValidPort(16888));
    }
}
