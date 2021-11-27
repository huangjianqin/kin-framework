/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * Forked from <a href="https://github.com/fengjiachun/Jupiter">Jupiter</a>.
 *
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeLongFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements LongFieldUpdater<U> {
    UnsafeLongFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, long newValue) {
        if (volatileField) {
            unsafe.putLongVolatile(obj, offset, newValue);
        } else {
            unsafe.putLong(obj, offset, newValue);
        }
    }

    @Override
    public long get(U obj) {
        if (volatileField) {
            return unsafe.getLongVolatile(obj, offset);
        } else {
            return unsafe.getLong(obj, offset);
        }
    }
}
