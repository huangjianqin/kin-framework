package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeBooleanFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements BooleanFieldUpdater<U> {
    UnsafeBooleanFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, boolean newValue) {
        if (volatileField) {
            unsafe.putBooleanVolatile(obj, offset, newValue);
        } else {
            unsafe.putBoolean(obj, offset, newValue);
        }
    }

    @Override
    public boolean get(U obj) {
        if (volatileField) {
            return unsafe.getBooleanVolatile(obj, offset);
        } else {
            return unsafe.getBoolean(obj, offset);
        }
    }
}
