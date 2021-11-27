package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
public interface BooleanFieldUpdater<U> {
    void set(U obj, boolean newValue);

    boolean get(U obj);
}
