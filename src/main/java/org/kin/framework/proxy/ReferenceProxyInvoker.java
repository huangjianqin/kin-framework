package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 方法代理类
 * 底层简单粗暴利用反射实现代理调用
 *
 * @author huangjianqin
 * @date 2020-01-12
 */
public class ReferenceProxyInvoker<S> implements ProxyInvoker<S> {
    private final S proxyObj;
    private final Method method;

    public ReferenceProxyInvoker(S proxyObj, Method method, String packageName) {
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
