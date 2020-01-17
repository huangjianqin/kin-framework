package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2020-01-16
 */
@FunctionalInterface
public interface MethodBodyConstructor {
    String construct(String proxyFieldName, Method proxyMethod);
}
