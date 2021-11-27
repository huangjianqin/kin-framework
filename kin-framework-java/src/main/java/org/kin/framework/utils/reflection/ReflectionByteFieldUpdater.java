package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class ReflectionByteFieldUpdater<U> extends AbstractReflectionFieldUpdater implements ByteFieldUpdater<U> {
    ReflectionByteFieldUpdater(Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(tClass, fieldName);
    }

    @Override
    public void set(U obj, byte newValue) {
        set0(obj, newValue);
    }

    @Override
    public byte get(U obj) {
        return (byte) get0(obj);
    }
}
