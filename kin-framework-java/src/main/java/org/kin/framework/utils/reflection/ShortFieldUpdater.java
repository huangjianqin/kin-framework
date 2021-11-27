package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
public interface ShortFieldUpdater<U> {
    void set(U obj, short newValue);

    short get(U obj);
}
