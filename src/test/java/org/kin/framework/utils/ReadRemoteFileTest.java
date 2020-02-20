package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class ReadRemoteFileTest {
    public static void main(String[] args) {
        System.out.println(NetUtils.copyRemoteFile("http://www.baidu.com", "test"));
    }
}
