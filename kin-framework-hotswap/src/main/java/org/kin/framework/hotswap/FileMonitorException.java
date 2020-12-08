package org.kin.framework.hotswap;

/**
 * @author huangjianqin
 * @date 2020/12/8
 */
public class FileMonitorException extends RuntimeException {
    private static final long serialVersionUID = -577314261845725817L;

    public FileMonitorException(String message) {
        super(message);
    }

    public FileMonitorException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileMonitorException(Throwable cause) {
        super(cause);
    }
}
