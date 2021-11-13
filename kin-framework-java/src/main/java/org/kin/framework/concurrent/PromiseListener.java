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
 * Listens to the result of a {@link Promise}.  The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Promise#addListener(PromiseListener)}.
 * <p>
 * 监听异步操作结果
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
public interface PromiseListener<P extends Promise<?>> {

    /**
     * Invoked when the operation associated with the {@link Promise} has been completed.
     *
     * @param promise the source {@link Promise} which called this callback
     */
    void onComplete(P promise) throws Exception;
}
