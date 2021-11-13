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

import java.util.Arrays;
import java.util.concurrent.Future;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
final class DefaultPromiseListeners {
    private PromiseListener<? extends Promise<?>>[] listeners;
    /** {@link PromiseListener}数量 */
    private int size;
    /** {@link ProgressivePromiseListener}数量 */
    private int progressiveSize;

    @SuppressWarnings("unchecked")
    DefaultPromiseListeners(
            PromiseListener<? extends Promise<?>> first, PromiseListener<? extends Promise<?>> second) {
        listeners = new PromiseListener[2];
        listeners[0] = first;
        listeners[1] = second;
        size = 2;

        if (first instanceof ProgressivePromiseListener) {
            progressiveSize++;
        }
        if (second instanceof ProgressivePromiseListener) {
            progressiveSize++;
        }
    }

    /**
     * add listener
     */
    public void add(PromiseListener<? extends Promise<?>> listener) {
        PromiseListener<? extends Promise<?>>[] listeners = this.listeners;
        int size = this.size;
        if (size == listeners.length) {
            //扩容
            this.listeners = listeners = Arrays.copyOf(listeners, size << 1);
        }
        //赋值
        listeners[size] = listener;
        this.size = size + 1;

        if (listener instanceof ProgressivePromiseListener) {
            progressiveSize++;
        }
    }

    /**
     * remove listener
     */
    public void remove(PromiseListener<? extends Future<?>> listener) {
        PromiseListener<? extends Future<?>>[] listeners = this.listeners;
        int size = this.size;
        for (int i = 0; i < size; i++) {
            if (listeners[i] == listener) {
                int listenersToMove = size - i - 1;
                if (listenersToMove > 0) {
                    System.arraycopy(listeners, i + 1, listeners, i, listenersToMove);
                }

                //因为移动了, 因此需要将tail listener set为null
                listeners[--size] = null;
                this.size = size;

                if (listener instanceof ProgressivePromiseListener) {
                    progressiveSize--;
                }
                return;
            }
        }
    }

    //getter
    public PromiseListener<? extends Promise<?>>[] listeners() {
        return listeners;
    }

    public int size() {
        return size;
    }

    public int progressiveSize() {
        return progressiveSize;
    }
}
