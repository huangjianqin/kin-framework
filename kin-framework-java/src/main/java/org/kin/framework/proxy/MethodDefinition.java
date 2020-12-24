package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 代理方法定义
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public class MethodDefinition<T> {
    /** 实现类 */
    private final T service;
    /** 代理方法 */
    private final Method method;

    public MethodDefinition(T service, Method method) {
        this.service = service;
        this.method = method;
    }

    //getter
    public T getService() {
        return service;
    }

    public Method getMethod() {
        return method;
    }
}
