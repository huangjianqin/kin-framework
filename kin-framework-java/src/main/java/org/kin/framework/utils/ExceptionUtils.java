package org.kin.framework.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author huangjianqin
 * @date 2018/1/28
 */
public class ExceptionUtils {
    public static String getExceptionDesc(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
