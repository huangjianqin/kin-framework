package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class WorkingDirectoryNotExistsException extends RuntimeException {
    public WorkingDirectoryNotExistsException(String message) {
        super(message);
    }
}
