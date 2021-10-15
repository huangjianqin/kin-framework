package org.kin.framework.concurrent;

import org.kin.framework.utils.StringUtils;
import org.springframework.lang.NonNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2019/3/1
 */
public class SimpleThreadFactory implements ThreadFactory {
    private final AtomicInteger counter = new AtomicInteger(1);
    private final String prefix;
    private final boolean daemon;
    private final int priority;
    private final ThreadGroup threadGroup;

    public SimpleThreadFactory(String prefix) {
        this(prefix, false, Thread.MIN_PRIORITY);
    }

    public SimpleThreadFactory(String prefix, boolean daemon) {
        this(prefix, daemon, Thread.MIN_PRIORITY);
    }

    public SimpleThreadFactory(String prefix, int priority) {
        this(prefix, false, priority);
    }

    public SimpleThreadFactory(String prefix, boolean daemon, int priority) {
        this(prefix, daemon, priority, Threads.getThreadGroup());
    }

    public SimpleThreadFactory(String prefix, boolean daemon, int priority, ThreadGroup threadGroup) {
        if (StringUtils.isBlank(prefix)) {
            throw new IllegalArgumentException("prefix is not blank");
        }

        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "priority: " + priority + " (expected: Thread.MIN_PRIORITY <= priority <= Thread.MAX_PRIORITY)");
        }

        this.prefix = prefix;
        this.daemon = daemon;
        this.priority = priority;
        this.threadGroup = threadGroup;
    }

    @Override
    @NonNull
    public final Thread newThread(Runnable r) {
        Thread thread = newThread(threadGroup, r, prefix + "--threads-", counter.getAndIncrement());
        if (thread.isDaemon()) {
            if (!daemon) {
                thread.setDaemon(false);
            }
        } else {
            if (daemon) {
                thread.setDaemon(true);
            }
        }
        if (thread.getPriority() != priority) {
            thread.setPriority(priority);
        }
        return thread;
    }

    /**
     * 供子类自实现的thread
     */
    protected Thread newThread(ThreadGroup threadGroup, Runnable r, String prefix, int count) {
        return new Thread(threadGroup, r, prefix + count);
    }

    //getter
    public String getPrefix() {
        return prefix;
    }

    public boolean isDaemon() {
        return daemon;
    }

    public int getPriority() {
        return priority;
    }
}
