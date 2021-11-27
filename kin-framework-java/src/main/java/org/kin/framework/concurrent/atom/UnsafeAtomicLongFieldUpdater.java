package org.kin.framework.concurrent.atom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeAtomicLongFieldUpdater<U> extends AtomicLongFieldUpdater<U> {
    private final long offset;
    private final Unsafe unsafe;

    UnsafeAtomicLongFieldUpdater(Unsafe unsafe, Class<U> tClass, String fieldName) throws NoSuchFieldException {
        Field field = tClass.getDeclaredField(fieldName);
        if (!Modifier.isVolatile(field.getModifiers())) {
            throw new IllegalArgumentException("Field [" + fieldName + "] must be volatile");
        }
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public boolean compareAndSet(U obj, long expect, long update) {
        return unsafe.compareAndSwapLong(obj, offset, expect, update);
    }

    @Override
    public boolean weakCompareAndSet(U obj, long expect, long update) {
        return compareAndSet(obj, expect, update);
    }

    @Override
    public void set(U obj, long newValue) {
        unsafe.putLongVolatile(obj, offset, newValue);
    }

    @Override
    public void lazySet(U obj, long newValue) {
        unsafe.putOrderedLong(obj, offset, newValue);
    }

    @Override
    public long get(U obj) {
        return unsafe.getLongVolatile(obj, offset);
    }
}
