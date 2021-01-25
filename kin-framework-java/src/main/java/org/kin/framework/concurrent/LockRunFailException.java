package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2020-01-15
 */
public class LockRunFailException extends RuntimeException {
    public LockRunFailException() {
        super();
    }

    public LockRunFailException(String message) {
        super(message);
    }
}
