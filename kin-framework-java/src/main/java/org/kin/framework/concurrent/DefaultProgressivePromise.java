/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.kin.framework.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
public class DefaultProgressivePromise<V> extends DefaultPromise<V> implements ProgressivePromise<V> {
    private static Logger log = LoggerFactory.getLogger(DefaultProgressivePromise.class);

    /**
     * Creates a new instance.
     * <p>
     * It is preferable to use {@link EventExecutor#newProgressivePromise()} to create a new progressive promise
     *
     * @param executor the {@link EventExecutor} which is used to notify the promise when it progresses or it is complete
     */
    public DefaultProgressivePromise(EventExecutor executor) {
        super(executor);
    }

    public DefaultProgressivePromise() {
    }

    @Override
    public ProgressivePromise<V> setProgress(long progress, long total) {
        if (total < 0) {
            // total unknown
            total = -1; // normalize
            if (progress < 0) {
                throw new IllegalArgumentException("progress: " + progress + " (expected: >= 0)");
            }
        } else if (progress < 0 || progress > total) {
            throw new IllegalArgumentException(
                    "progress: " + progress + " (expected: 0 <= progress <= total (" + total + "))");
        }

        if (isDone()) {
            throw new IllegalStateException("complete already");
        }

        notifyProgressiveListeners(progress, total);
        return this;
    }

    @Override
    public boolean tryProgress(long progress, long total) {
        if (total < 0) {
            total = -1;
            if (progress < 0 || isDone()) {
                return false;
            }
        } else if (progress < 0 || progress > total || isDone()) {
            return false;
        }

        notifyProgressiveListeners(progress, total);
        return true;
    }

    /**
     * Notify all progressive listeners.
     * <p>
     * No attempt is made to ensure notification order if multiple calls are made to this method before
     * the original invocation completes.
     * <p>
     * This will do an iteration over all listeners to get all of type {@link ProgressivePromiseListener}s.
     * <p>
     * 以指定进度{@code progress}和总值{@code total}触发已注册的{@link ProgressivePromiseListener}
     * 不保证listener注册顺序
     *
     * @param progress the new progress.
     * @param total    the total progress.
     */
    @SuppressWarnings("unchecked")
    void notifyProgressiveListeners(long progress, long total) {
        Object listeners = progressiveListeners();
        if (listeners == null) {
            return;
        }

        Executor executor = executor();
        if (executor instanceof EventExecutor) {
            EventExecutor eventExecutor = (EventExecutor) executor;
            if (eventExecutor.isInEventLoop()) {
                if (listeners instanceof ProgressivePromiseListener[]) {
                    notifyProgressiveListeners0((ProgressivePromiseListener<?>[]) listeners, progress, total);
                } else {
                    notifyProgressiveListener0((ProgressivePromiseListener<ProgressivePromise<V>>) listeners, progress, total);
                }
                return;
            }
        }

        if (listeners instanceof ProgressivePromiseListener[]) {
            ProgressivePromiseListener<?>[] array = (ProgressivePromiseListener<?>[]) listeners;
            safeExecute(executor, () -> notifyProgressiveListeners0(array, progress, total));
        } else {
            ProgressivePromiseListener<ProgressivePromise<V>> l = (ProgressivePromiseListener<ProgressivePromise<V>>) listeners;
            safeExecute(executor, () -> notifyProgressiveListener0(l, progress, total));
        }
    }

    /**
     * Returns a {@link ProgressivePromiseListener}, an array of {@link ProgressivePromiseListener}, or {@code null}.
     * <p>
     * 获取已注册的{@link ProgressivePromiseListener}
     */
    private synchronized Object progressiveListeners() {
        Object listeners = this.listeners;
        if (listeners == null) {
            // No listeners added
            return null;
        }

        if (listeners instanceof DefaultPromiseListeners) {
            // Copy DefaultFutureListeners into an array of listeners.
            //过滤出GenericProgressivePromiseListener array
            DefaultPromiseListeners dfl = (DefaultPromiseListeners) listeners;
            int progressiveSize = dfl.progressiveSize();
            if (progressiveSize == 0) {
                //没listener
                return null;
            } else if (progressiveSize == 1) {
                //1个listener
                for (PromiseListener<?> l : dfl.listeners()) {
                    if (l instanceof ProgressivePromiseListener) {
                        return l;
                    }
                }
                return null;
            } else {
                //多个listener
                PromiseListener<?>[] array = dfl.listeners();
                ProgressivePromiseListener<?>[] copy = new ProgressivePromiseListener[progressiveSize];
                for (int i = 0, j = 0; j < progressiveSize; i++) {
                    PromiseListener<?> l = array[i];
                    if (l instanceof ProgressivePromiseListener) {
                        copy[j++] = (ProgressivePromiseListener<?>) l;
                    }
                }

                return copy;
            }
        } else if (listeners instanceof ProgressivePromiseListener) {
            //单个GenericProgressivePromiseListener
            return listeners;
        } else {
            // Only one listener was added and it's not a progressive listener.
            return null;
        }
    }

    /**
     * 批量触发listener
     */
    private void notifyProgressiveListeners0(ProgressivePromiseListener<?>[] listeners, long progress, long total) {
        for (ProgressivePromiseListener<?> l : listeners) {
            if (l == null) {
                break;
            }
            notifyProgressiveListener0(l, progress, total);
        }
    }

    /**
     * 触发listener
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void notifyProgressiveListener0(ProgressivePromiseListener l, long progress, long total) {
        try {
            l.onProgressed(this, progress, total);
        } catch (Throwable t) {
            if (log.isWarnEnabled()) {
                log.warn("an exception was thrown by " + l.getClass().getName() + ".onProgressed()", t);
            }
        }
    }

    @Override
    public ProgressivePromise<V> addListener(PromiseListener<? extends Promise<? super V>> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public ProgressivePromise<V> addListeners(PromiseListener<? extends Promise<? super V>>... listeners) {
        super.addListeners(listeners);
        return this;
    }

    @Override
    public ProgressivePromise<V> removeListener(PromiseListener<? extends Promise<? super V>> listener) {
        super.removeListener(listener);
        return this;
    }

    @Override
    public ProgressivePromise<V> removeListeners(PromiseListener<? extends Promise<? super V>>... listeners) {
        super.removeListeners(listeners);
        return this;
    }

    @Override
    public ProgressivePromise<V> sync() throws InterruptedException {
        super.sync();
        return this;
    }

    @Override
    public ProgressivePromise<V> syncUninterruptibly() {
        super.syncUninterruptibly();
        return this;
    }

    @Override
    public ProgressivePromise<V> await() throws InterruptedException {
        super.await();
        return this;
    }

    @Override
    public ProgressivePromise<V> awaitUninterruptibly() {
        super.awaitUninterruptibly();
        return this;
    }

    @Override
    public ProgressivePromise<V> success(V result) {
        super.success(result);
        return this;
    }

    @Override
    public ProgressivePromise<V> failure(Throwable cause) {
        super.failure(cause);
        return this;
    }
}