package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
final class ReflectionCharFieldUpdater<U> extends AbstractReflectionFieldUpdater implements CharFieldUpdater<U> {
    ReflectionCharFieldUpdater(Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        super(tClass, fieldName);
    }

    @Override
    public void set(U obj, char newValue) {
        set0(obj, newValue);
    }

    @Override
    public char get(U obj) {
        return (char) get0(obj);
    }
}
