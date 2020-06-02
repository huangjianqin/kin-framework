package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-06-02
 */
public class CopyRemoteFileTest {
    public static void main(String[] args) {
        NetUtils.copyRemoteFile("https://www.baidu.com", "out/copy.txt");
    }
}
