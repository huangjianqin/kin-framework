package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 方法代理定义
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public class ProxyMethodDefinition {
    private Object proxyObj;
    private Method method;
    private String className;

    public ProxyMethodDefinition(Object proxyObj, Method method, String packageName) {
        this(proxyObj, method, packageName, proxyObj.getClass().getSimpleName().concat("$").concat(method.getName()));
    }

    public ProxyMethodDefinition(Object proxyObj, Method method, String packageName, String uniqueMethodName) {
        this.proxyObj = proxyObj;
        this.method = method;
        this.className = packageName.concat(".").concat(uniqueMethodName).concat("$JavassistProxy");
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