package org.kin.framework.utils;

/**
 * json反序列化异常
 *
 * @author huangjianqin
 * @date 2020/12/8
 */
public class JsonDeserializeException extends RuntimeException {
    public JsonDeserializeException(String message) {
        super(message);
    }

    public JsonDeserializeException(String message, Throwable cause) {
        super(message, cause);
    }

    public JsonDeserializeException(Throwable cause) {
        super(cause);
    }
}
