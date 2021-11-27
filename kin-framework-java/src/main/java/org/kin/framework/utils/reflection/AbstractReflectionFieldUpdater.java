package org.kin.framework.utils.reflection;

import java.lang.reflect.Field;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
abstract class AbstractReflectionFieldUpdater {
    protected final Field field;

    public AbstractReflectionFieldUpdater(Class<?> tClass, String fieldName) throws NoSuchFieldException {
        this.field = tClass.getDeclaredField(fieldName);
        this.field.setAccessible(true);
    }

    protected void set0(Object obj, Object newValue) {
        try {
            field.set(obj, newValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected Object get0(Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
