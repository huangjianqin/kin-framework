package org.kin.framework.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2021/11/22
 */
public class PowerOfTwoEventExecutorChooser implements EventExecutorChooser {
    private final AtomicInteger idx = new AtomicInteger();

    @Override
    public EventExecutor choose(EventExecutor[] executors) {
        return executors[this.idx.getAndIncrement() & executors.length - 1];
    }
}
