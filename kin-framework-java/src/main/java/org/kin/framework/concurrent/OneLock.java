package org.kin.framework.concurrent;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author huangjianqin
 * @date 2020-06-14
 */
public class OneLock extends AbstractQueuedSynchronizer {
    private final int DONE = 1;
    private final int PENDING = 0;

    @Override
    protected boolean tryAcquire(int acquires) {
        return getState() == DONE;
    }

    @Override
    protected boolean tryRelease(int releases) {
        if (getState() == PENDING) {
            return compareAndSetState(PENDING, DONE);
        }

        return false;
    }

    public boolean isDone() {
        return getState() == DONE;
    }
}
