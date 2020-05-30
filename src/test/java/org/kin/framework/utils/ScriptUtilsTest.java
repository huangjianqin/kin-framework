package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-05-30
 */
public class ScriptUtilsTest {
    public static void main(String[] args) {
        try {
            ScriptUtils.execCommand("ls -la");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
