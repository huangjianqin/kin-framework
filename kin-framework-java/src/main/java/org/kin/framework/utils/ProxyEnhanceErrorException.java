package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020/12/9
 */
public class ProxyEnhanceErrorException extends RuntimeException {
    private static final long serialVersionUID = -8797277253882249868L;

    public ProxyEnhanceErrorException(String message) {
        super(message);
    }

    public ProxyEnhanceErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProxyEnhanceErrorException(Throwable cause) {
        super(cause);
    }
}
