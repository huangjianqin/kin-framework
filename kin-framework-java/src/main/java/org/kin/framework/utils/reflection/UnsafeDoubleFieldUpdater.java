package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeDoubleFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements DoubleFieldUpdater<U> {
    UnsafeDoubleFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, double newValue) {
        if (volatileField) {
            unsafe.putDoubleVolatile(obj, offset, newValue);
        } else {
            unsafe.putDouble(obj, offset, newValue);
        }
    }

    @Override
    public double get(U obj) {
        if (volatileField) {
            return unsafe.getDoubleVolatile(obj, offset);
        } else {
            return unsafe.getDouble(obj, offset);
        }
    }
}
