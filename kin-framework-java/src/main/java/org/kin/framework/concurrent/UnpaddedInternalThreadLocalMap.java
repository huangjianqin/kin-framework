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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The internal data structure that stores the thread-local variables for Netty and all {@link FastThreadLocal}s.
 * Note that this class is for internal use only and is subject to change at any time.  Use {@link FastThreadLocal}
 * unless you know what you are doing.
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
class UnpaddedInternalThreadLocalMap {
    /** 支持回退至获取java原生支持的ThreadLocal */
    static final ThreadLocal<InternalThreadLocalMap> slowThreadLocalMap = new ThreadLocal<InternalThreadLocalMap>();
    /** {@link FastThreadLocal}唯一index生成器 */
    static final AtomicInteger nextIndex = new AtomicInteger();

    /** Used by {@link FastThreadLocal}, FastThreadLocal value */
    Object[] indexedVariables;

    /** thread local random */
    FastThreadLocalRandom random;

    /**
     * 防止{@link EventExecutor}内不断触发{@link Promise}导致{@link StackOverflowError}
     */
    int promiseListenerStackDepth;

    UnpaddedInternalThreadLocalMap(Object[] indexedVariables) {
        this.indexedVariables = indexedVariables;
    }
}
