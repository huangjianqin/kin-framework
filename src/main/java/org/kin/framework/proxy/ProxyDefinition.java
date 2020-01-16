package org.kin.framework.proxy;

/**
 * @author huangjianqin
 * @date 2020-01-16
 */
public class ProxyDefinition {
    private Object proxyObj;
    private String packageName;

    public ProxyDefinition(Object proxyObj, String packageName) {
        this.proxyObj = proxyObj;
        this.packageName = packageName;
    }

    //getter
    public Object getProxyObj() {
        return proxyObj;
    }

    public String getPackageName() {
        return packageName;
    }
}
