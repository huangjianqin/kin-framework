package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeShortFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements ShortFieldUpdater<U> {
    UnsafeShortFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, short newValue) {
        if (volatileField) {
            unsafe.putShortVolatile(obj, offset, newValue);
        } else {
            unsafe.putShort(obj, offset, newValue);
        }
    }

    @Override
    public short get(U obj) {
        if (volatileField) {
            return unsafe.getShortVolatile(obj, offset);
        } else {
            return unsafe.getShort(obj, offset);
        }
    }
}
