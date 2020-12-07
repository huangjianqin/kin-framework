package org.kin.framework.utils;

/**
 * json序列化异常
 *
 * @author huangjianqin
 * @date 2020/12/8
 */
public class JsonSerializeException extends RuntimeException {
    public JsonSerializeException(String message) {
        super(message);
    }

    public JsonSerializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonSerializeException(Throwable cause) {
        super(cause);
    }
}
