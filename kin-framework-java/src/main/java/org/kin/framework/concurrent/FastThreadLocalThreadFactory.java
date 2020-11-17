package org.kin.framework.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * A {@link ThreadFactory} implementation with a simple naming rule.
 */
public class FastThreadLocalThreadFactory extends SimpleThreadFactory {
    public FastThreadLocalThreadFactory(String prefix) {
        this(prefix, false, Thread.MIN_PRIORITY);
    }

    public FastThreadLocalThreadFactory(String prefix, boolean daemon) {
        this(prefix, daemon, Thread.MIN_PRIORITY);
    }

    public FastThreadLocalThreadFactory(String prefix, int priority) {
        this(prefix, false, priority);
    }

    public FastThreadLocalThreadFactory(String prefix, boolean daemon, int priority) {
        this(prefix, daemon, priority, Threads.getThreadGroup());
    }

    public FastThreadLocalThreadFactory(String prefix, boolean daemon, int priority, ThreadGroup threadGroup) {
        super(prefix, daemon, priority, threadGroup);
    }


    @Override
    protected Thread newThread(ThreadGroup threadGroup, Runnable r, String prefix, int count) {
        return new FastThreadLocalThread(threadGroup, new RunnableDecorator(r), prefix + count);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 包装Runnable, 该Runnable执行完后会移除所有FastThreadLocal
     */
    private static final class RunnableDecorator implements Runnable {

        private final Runnable r;

        RunnableDecorator(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            try {
                r.run();
            } finally {
                FastThreadLocal.removeAll();
            }
        }
    }
}
