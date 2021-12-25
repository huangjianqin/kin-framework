package org.kin.framework.beans;

/**
 * class field复制接口
 *
 * @author huangjianqin
 * @date 2021/9/8
 */
public interface Copy<S, T> {
    /**
     * 将source的field 字段值复制到target 对应field
     * 同时支持浅复制(引用复制)和深复制(值复制)
     *
     * @param source 源bean
     * @param target 目标bean
     * @see BeanUtils#DEEP
     */
    void copyProperties(S source, T target);
}
