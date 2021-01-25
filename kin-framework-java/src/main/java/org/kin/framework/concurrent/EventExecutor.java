package org.kin.framework.concurrent;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @author huangjianqin
 * @date 2021/1/25
 */
public interface EventExecutor extends ScheduledExecutorService {
    /** 是否在同一线程loop */
    boolean isInEventLoop();

    /** 是否在同一线程loop */
    boolean isInEventLoop(Thread thread);
}
