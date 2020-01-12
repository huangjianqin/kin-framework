package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2020-01-11
 */
public class ProxyDefinition<T> {
    private Object proxyObj;
    private Method method;
    private String className;

    public ProxyDefinition(Object proxyObj, Method method, String packageName) {
        this(proxyObj, method, packageName, proxyObj.getClass().getSimpleName() + "$" + method.getName());
    }

    public ProxyDefinition(Object proxyObj, Method method, String packageName, String uniqueMethodName) {
        this.proxyObj = proxyObj;
        this.method = method;
        this.className = packageName + uniqueMethodName + "$JavassistProxy";
    }

    //getter
    public Object getProxyObj() {
        return proxyObj;
    }

    public Method getMethod() {
        return method;
    }

    public String getClassName() {
        return className;
    }
}
