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

import org.kin.framework.utils.UnsafeUtil;

/**
 * 基于反射或者{@link sun.misc.Unsafe}更新Field value策略工厂方法
 * <p>
 * Forked from <a href="https://github.com/fengjiachun/Jupiter">Jupiter</a>.
 *
 * @author huangjianqin
 * @date 2021/11/27
 */
public class FieldUpdaters {

    /**
     * Creates and returns an updater for integer with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> IntegerFieldUpdater<U> newIntegerFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeIntegerFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionIntegerFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for long with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> LongFieldUpdater<U> newLongFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeLongFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionLongFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for boolean with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> BooleanFieldUpdater<U> newBooleanFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeBooleanFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionBooleanFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for byte with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> ByteFieldUpdater<U> newByteFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeByteFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionByteFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for long with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> CharFieldUpdater<U> newCharFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeCharFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionCharFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for short with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> ShortFieldUpdater<U> newShortFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeShortFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionShortFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for float with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> FloatFieldUpdater<U> newFloatFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeFloatFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionFloatFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for double with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U> DoubleFieldUpdater<U> newDoubleFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeDoubleFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionDoubleFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates and returns an updater for objects with the given field.
     *
     * @param tClass    the class of the objects holding the field.
     * @param fieldName the name of the field to be updated.
     */
    public static <U, W> ReferenceFieldUpdater<U, W> newReferenceFieldUpdater(Class<? super U> tClass, String fieldName) {
        try {
            if (UnsafeUtil.hasUnsafe()) {
                return new UnsafeReferenceFieldUpdater<>(UnsafeUtil.getUnsafeAccessor().getUnsafe(), tClass, fieldName);
            } else {
                return new ReflectionReferenceFieldUpdater<>(tClass, fieldName);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
