package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class ReflectionFloatFieldUpdater<U> extends AbstractReflectionFieldUpdater implements FloatFieldUpdater<U> {
    ReflectionFloatFieldUpdater(Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(tClass, fieldName);
    }

    @Override
    public void set(U obj, float newValue) {
        set0(obj, newValue);
    }

    @Override
    public float get(U obj) {
        return (float) get0(obj);
    }
}
