package org.kin.framework.concurrent;

/**
 * An {@link IllegalStateException} which is raised when a user performed a blocking operation
 * when the user is in an event loop thread.  If a blocking operation is performed in an event loop
 * thread, the blocking operation will most likely enter a dead lock state, hence throwing this
 * exception.
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/13
 */
public class BlockingOperationException extends IllegalStateException {
    private static final long serialVersionUID = 6760171289763658600L;

    public BlockingOperationException() {
    }

    public BlockingOperationException(String s) {
        super(s);
    }

    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockingOperationException(Throwable cause) {
        super(cause);
    }
}
