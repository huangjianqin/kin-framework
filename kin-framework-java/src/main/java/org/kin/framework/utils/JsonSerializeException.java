package org.kin.framework.utils;

/**
 * json序列化异常
 *
 * @author huangjianqin
 * @date 2020/12/8
 */
public class JsonSerializeException extends RuntimeException {
    private static final long serialVersionUID = -2196023913662176394L;

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
