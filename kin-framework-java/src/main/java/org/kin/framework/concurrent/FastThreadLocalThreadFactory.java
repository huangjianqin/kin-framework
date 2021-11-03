/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.kin.framework.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * A {@link ThreadFactory} implementation with a simple naming rule.
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
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
        return new FastThreadLocalThread(threadGroup, new RunnableWrapper(r), prefix + count);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 包装Runnable, 该Runnable执行完后会移除所有FastThreadLocal
     */
    private static final class RunnableWrapper implements Runnable {

        private final Runnable r;

        RunnableWrapper(Runnable r) {
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
