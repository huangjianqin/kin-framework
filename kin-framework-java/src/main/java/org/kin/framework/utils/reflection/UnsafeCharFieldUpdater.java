package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeCharFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements CharFieldUpdater<U> {
    UnsafeCharFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, char newValue) {
        if (volatileField) {
            unsafe.putCharVolatile(obj, offset, newValue);
        } else {
            unsafe.putChar(obj, offset, newValue);
        }
    }

    @Override
    public char get(U obj) {
        if (volatileField) {
            return unsafe.getCharVolatile(obj, offset);
        } else {
            return unsafe.getChar(obj, offset);
        }
    }
}
