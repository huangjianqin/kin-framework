package org.kin.framework.proxy;

/**
 * 代理类定义
 *
 * @author huangjianqin
 * @date 2020-01-16
 */
public class ProxyDefinition<T> {
    private static final MethodBodyConstructor DEFAULT_METHOD_BODY_CONSTRUCTOR =
            ProxyEnhanceUtils::generateProxyInvokeCode;
    //---------------------------------------------------------------------------------------------------------------------
    /** 实现类 */
    private T proxyObj;
    /** 包名 */
    private String packageName;
    /** 代理类中实现类调用方法代码定义 */
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
