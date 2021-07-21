package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 基于反射
 *
 * @author huangjianqin
 * @date 2020-01-12
 */
class ReflectionInvoker<S> implements ProxyInvoker<S> {
    /** 实例 */
    private final S proxyObj;
    /** 目标方法 */
    private final Method method;

    public ReflectionInvoker(S proxyObj, Method method) {
        this.proxyObj = proxyObj;
        this.method = method;
    }

    @Override
    public S getProxyObj() {
        return proxyObj;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object invoke(Object... params) throws Exception {
        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        return method.invoke(proxyObj, params);
    }
}
