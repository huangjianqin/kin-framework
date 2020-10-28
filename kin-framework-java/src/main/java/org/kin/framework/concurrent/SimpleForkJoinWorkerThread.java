package org.kin.framework.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * @author huangjianqin
 * @date 2020-03-16
 */
public class SimpleForkJoinWorkerThread extends ForkJoinWorkerThread {
    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     * @throws NullPointerException if pool is null
     */
    protected SimpleForkJoinWorkerThread(ForkJoinPool pool) {
        super(pool);
    }
}
