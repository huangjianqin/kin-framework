package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeFloatFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements FloatFieldUpdater<U> {
    UnsafeFloatFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, float newValue) {
        if (volatileField) {
            unsafe.putFloatVolatile(obj, offset, newValue);
        } else {
            unsafe.putFloat(obj, offset, newValue);
        }
    }

    @Override
    public float get(U obj) {
        if (volatileField) {
            return unsafe.getFloatVolatile(obj, offset);
        } else {
            return unsafe.getFloat(obj, offset);
        }
    }
}
