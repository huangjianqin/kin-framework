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
 * 关联{@link Promise}进度发生变化时触发的listener
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
public interface ProgressivePromiseListener<P extends ProgressivePromise<?>> extends PromiseListener<P> {
    /**
     * Invoked when the operation has progressed.
     * <p>
     * 当操作进度发生变化时触发
     *
     * @param progress the progress of the operation so far (cumulative)
     * @param total    the number that signifies the end of the operation when {@code progress} reaches at it.
     *                 {@code -1} if the end of operation is unknown.
     */
    void onProgressed(P promise, long progress, long total) throws Exception;
}
