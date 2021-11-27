package org.kin.framework.concurrent.atom;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeAtomicIntegerFieldUpdater<U> extends AtomicIntegerFieldUpdater<U> {
    private final long offset;
    private final Unsafe unsafe;

    UnsafeAtomicIntegerFieldUpdater(Unsafe unsafe, Class<U> tClass, String fieldName) throws NoSuchFieldException {
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
    public boolean compareAndSet(U obj, int expect, int update) {
        return unsafe.compareAndSwapInt(obj, offset, expect, update);
    }

    @Override
    public boolean weakCompareAndSet(U obj, int expect, int update) {
        return compareAndSet(obj, expect, update);
    }

    @Override
    public void set(U obj, int newValue) {
        unsafe.putIntVolatile(obj, offset, newValue);
    }

    @Override
    public void lazySet(U obj, int newValue) {
        unsafe.putOrderedInt(obj, offset, newValue);
    }

    @Override
    public int get(U obj) {
        return unsafe.getIntVolatile(obj, offset);
    }
}
