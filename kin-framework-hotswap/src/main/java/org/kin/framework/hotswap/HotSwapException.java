package org.kin.framework.hotswap;

/**
 * @author huangjianqin
 * @date 2020/12/9
 */
public class HotSwapException extends RuntimeException {
    private static final long serialVersionUID = 6066529313575668786L;

    public HotSwapException(String message) {
        super(message);
    }

    public HotSwapException(String message, Throwable cause) {
        super(message, cause);
    }

    public HotSwapException(Throwable cause) {
        super(cause);
    }
}
