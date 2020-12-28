package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/4/3
 */
public class AsyncDbException extends RuntimeException {
    private static final long serialVersionUID = -5670487906248029951L;

    public AsyncDbException() {
    }

    public AsyncDbException(String message) {
        super(message);
    }

    public AsyncDbException(String message, Throwable cause) {
        super(message, cause);
    }

    public AsyncDbException(Throwable cause) {
        super(cause);
    }

    public AsyncDbException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
