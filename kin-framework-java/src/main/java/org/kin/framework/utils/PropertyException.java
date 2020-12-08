package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020/12/8
 */
public class PropertyException extends RuntimeException {
    private static final long serialVersionUID = 7907556667105175064L;

    public PropertyException() {
    }

    public PropertyException(String message) {
        super(message);
    }

    public PropertyException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyException(Throwable cause) {
        super(cause);
    }
}
