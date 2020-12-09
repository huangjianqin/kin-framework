package org.kin.framework.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author huangjianqin
 * @date 2018/1/28
 */
public class ExceptionUtils {
    /**
     * 获取异常描述
     */
    public static String getExceptionDesc(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 直接抛异常, 不需要外层提供异常捕获
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwExt(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
