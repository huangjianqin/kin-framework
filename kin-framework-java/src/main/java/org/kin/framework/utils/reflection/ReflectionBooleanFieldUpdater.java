package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class ReflectionBooleanFieldUpdater<U> extends AbstractReflectionFieldUpdater implements BooleanFieldUpdater<U> {
    ReflectionBooleanFieldUpdater(Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(tClass, fieldName);
    }

    @Override
    public void set(U obj, boolean newValue) {
        set0(obj, newValue);
    }

    @Override
    public boolean get(U obj) {
        return (boolean) get0(obj);
    }
}
