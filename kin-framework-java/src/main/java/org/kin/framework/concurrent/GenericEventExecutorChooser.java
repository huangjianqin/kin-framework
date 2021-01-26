package org.kin.framework.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 自增取模
 *
 * @author huangjianqin
 * @date 2021/1/26
 */
public final class GenericEventExecutorChooser implements EventExecutorChooser {
    private final AtomicLong idx = new AtomicLong();

    @Override
    public EventExecutor choose(EventExecutor[] executors) {
        return executors[(int) Math.abs(idx.getAndIncrement() % executors.length)];
    }
}
