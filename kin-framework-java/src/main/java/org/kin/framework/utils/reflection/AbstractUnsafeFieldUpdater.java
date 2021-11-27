package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
abstract class AbstractUnsafeFieldUpdater {
    protected final Unsafe unsafe;
    /** 是否是volatile字段 */
    protected final boolean volatileField;
    /** field 在类定义中的偏移量 */
    protected final long offset;

    protected AbstractUnsafeFieldUpdater(Unsafe unsafe, Class<?> tClass, String fieldName) throws NoSuchFieldException {
        this(unsafe, tClass.getDeclaredField(fieldName));
    }

    protected AbstractUnsafeFieldUpdater(Unsafe unsafe, Field field) {
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        offset = unsafe.objectFieldOffset(field);
        volatileField = Modifier.isVolatile(field.getModifiers());
    }
}
