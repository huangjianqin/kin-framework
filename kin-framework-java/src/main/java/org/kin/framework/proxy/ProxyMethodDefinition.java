package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 方法代理定义
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public class ProxyMethodDefinition<T> {
    /** 实现类 */
    private T proxyObj;
    /** 代理方法 */
    private Method method;
    /** 生成的代理类名 */
    private String className;

    public ProxyMethodDefinition(T proxyObj, Method method, String packageName) {
        this(proxyObj, method, packageName, proxyObj.getClass().getSimpleName().concat("$").concat(method.getName()));
    }

    public ProxyMethodDefinition(T proxyObj, Method method, String packageName, String uniqueMethodName) {
        this.proxyObj = proxyObj;
        this.method = method;
        this.className = packageName.concat(".").concat(uniqueMethodName).concat("$JavassistProxy");
    }

    //getter
    public T getProxyObj() {
        return proxyObj;
    }

    public Method getMethod() {
        return method;
    }

    public String getClassName() {
        return className;
    }
}
