package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class ReflectionShortFieldUpdater<U> extends AbstractReflectionFieldUpdater implements ShortFieldUpdater<U> {
    ReflectionShortFieldUpdater(Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(tClass, fieldName);
    }

    @Override
    public void set(U obj, short newValue) {
        set0(obj, newValue);
    }

    @Override
    public short get(U obj) {
        return (short) get0(obj);
    }
}
