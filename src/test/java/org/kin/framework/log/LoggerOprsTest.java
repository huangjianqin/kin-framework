package org.kin.framework.log;

/**
 * @author huangjianqin
 * @date 2020/8/10
 */
public class LoggerOprsTest {
    public static void main(String[] args) {
        LoggerOprsImpl obj = new LoggerOprsImpl();
        obj.trace("{}, {}, {}", "a", "b", "c");
    }

    private static class LoggerOprsImpl implements LoggerOprs {

    }
}
