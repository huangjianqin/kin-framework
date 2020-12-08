package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020/12/8
 */
public class NetworkException extends RuntimeException {
    private static final long serialVersionUID = 264053174416462656L;

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkException(Throwable cause) {
        super(cause);
    }
}
