package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class ReflectionDoubleFieldUpdater<U> extends AbstractReflectionFieldUpdater implements DoubleFieldUpdater<U> {
    ReflectionDoubleFieldUpdater(Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(tClass, fieldName);
    }

    @Override
    public void set(U obj, double newValue) {
        set0(obj, newValue);
    }

    @Override
    public double get(U obj) {
        return (double) get0(obj);
    }
}
