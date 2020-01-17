package org.kin.framework.proxy;

import org.kin.framework.proxy.utils.ProxyEnhanceUtils;

/**
 * @author huangjianqin
 * @date 2020-01-16
 */
public class ProxyDefinition {
    private static final MethodBodyConstructor DEFAULT_METHOD_BODY_CONSTRUCTOR =
            (proxyFieldName, proxyMethod) -> ProxyEnhanceUtils.generateProxyInvokeCode(proxyFieldName, proxyMethod);
    //---------------------------------------------------------------------------------------------------------------------

    private Object proxyObj;
    private String packageName;
    private MethodBodyConstructor methodBodyConstructor = DEFAULT_METHOD_BODY_CONSTRUCTOR;

    public ProxyDefinition(Object proxyObj, String packageName) {
        this.proxyObj = proxyObj;
        this.packageName = packageName;
    }

    public ProxyDefinition(Object proxyObj, String packageName, MethodBodyConstructor methodBodyConstructor) {
        this.proxyObj = proxyObj;
        this.packageName = packageName;
        this.methodBodyConstructor = methodBodyConstructor;
    }

    //getter
    public Object getProxyObj() {
        return proxyObj;
    }

    public String getPackageName() {
        return packageName;
    }

    public MethodBodyConstructor getMethodBodyConstructor() {
        return methodBodyConstructor;
    }
}
