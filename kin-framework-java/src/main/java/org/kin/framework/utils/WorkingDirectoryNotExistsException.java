package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class WorkingDirectoryNotExistsException extends RuntimeException {
    private static final long serialVersionUID = -6951093693556604821L;

    public WorkingDirectoryNotExistsException(String message) {
        super(message);
    }
}
