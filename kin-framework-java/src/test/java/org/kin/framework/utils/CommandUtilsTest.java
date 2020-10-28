package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-05-30
 */
public class CommandUtilsTest {
    public static void main(String[] args) {
        try {
            CommandUtils.execCommand("ls -la");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
