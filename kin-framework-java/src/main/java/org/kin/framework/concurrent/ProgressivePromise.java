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

/**
 * 会支持按操作进度触发listener的{@link Promise}实现, 并不会记录操作进度{@code progress}, 该值应该由外部维护, 并不能回退
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
public interface ProgressivePromise<V> extends Promise<V> {
    /**
     * Sets the current progress of the operation and notifies the listeners that implement
     * {@link ProgressivePromiseListener}.
     * <p>
     * 以指定进度{@code progress}和总值{@code total}触发已注册的{@link ProgressivePromiseListener}
     */
    ProgressivePromise<V> setProgress(long progress, long total);

    /**
     * Tries to set the current progress of the operation and notifies the listeners that implement
     * {@link ProgressivePromiseListener}.  If the operation is already complete or the progress is out of range,
     * this method does nothing but returning {@code false}.
     * <p>
     * 尝试以指定进度{@code progress}和总值{@code total}触发已注册的{@link ProgressivePromiseListener}, 如果操作已经完成或者超过总值, 则do nothing, 但return {@code false}
     */
    boolean tryProgress(long progress, long total);
}
