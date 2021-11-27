package org.kin.framework.utils.reflection;

import sun.misc.Unsafe;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class UnsafeByteFieldUpdater<U> extends AbstractUnsafeFieldUpdater implements ByteFieldUpdater<U> {
    UnsafeByteFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(unsafe, tClass, fieldName);
    }

    @Override
    public void set(U obj, byte newValue) {
        if (volatileField) {
            unsafe.putByteVolatile(obj, offset, newValue);
        } else {
            unsafe.putByte(obj, offset, newValue);
        }
    }

    @Override
    public byte get(U obj) {
        if (volatileField) {
            return unsafe.getByteVolatile(obj, offset);
        } else {
            return unsafe.getByte(obj, offset);
        }
    }
}
