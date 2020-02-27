package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2020-01-16
 */
@FunctionalInterface
public interface MethodBodyConstructor {
    /**
     * @param proxyFieldName 代理字段名
     * @param proxyMethod    代理方法名
     * @return 代理方法代码
     */
    String construct(String proxyFieldName, Method proxyMethod);
}
