package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
public interface DoubleFieldUpdater<U> {
    void set(U obj, double newValue);

    double get(U obj);
}
