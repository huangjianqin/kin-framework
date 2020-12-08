package org.kin.framework.proxy;

/**
 * @author huangjianqin
 * @date 2020-01-16
 */
public class ProxyDefinition<T> {
    private static final MethodBodyConstructor DEFAULT_METHOD_BODY_CONSTRUCTOR =
            ProxyEnhanceUtils::generateProxyInvokeCode;
    //---------------------------------------------------------------------------------------------------------------------

    private T proxyObj;
    private String packageName;
    private MethodBodyConstructor methodBodyConstructor = DEFAULT_METHOD_BODY_CONSTRUCTOR;

    public ProxyDefinition(T proxyObj, String packageName) {
        this.proxyObj = proxyObj;
        this.packageName = packageName;
    }

    public ProxyDefinition(T proxyObj, String packageName, MethodBodyConstructor methodBodyConstructor) {
        this.proxyObj = proxyObj;
        this.packageName = packageName;
        this.methodBodyConstructor = methodBodyConstructor;
    }

    //getter
    public T getProxyObj() {
        return proxyObj;
    }

    public String getPackageName() {
        return packageName;
    }

    public MethodBodyConstructor getMethodBodyConstructor() {
        return methodBodyConstructor;
    }
}
