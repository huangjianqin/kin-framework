package org.kin.framework.proxy;

import org.kin.framework.utils.SysUtils;

import java.lang.reflect.Proxy;

/**
 * 基于jdk proxy
 *
 * @author huangjianqin
 * @date 2020/12/24
 */
public class JdkProxyFactory implements ProxyFactory {
    /** 单例 */
    public static JdkProxyFactory INSTANCE = new JdkProxyFactory();

    private JdkProxyFactory() {
    }

    @Override
    public <T> ProxyInvoker<T> enhanceMethod(MethodDefinition<T> definition) {
        return new JdkProxyInvoker<>(definition.getService(), definition.getMethod());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> P enhanceClass(ClassDefinition<P> definition) {
        Class<P> interfaceClass = definition.getInterfaceClass();
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("only support interface");
        }
        return (P) Proxy.newProxyInstance(SysUtils.getClassLoader(interfaceClass), new Class<?>[]{interfaceClass},
                (o, method, objects) -> method.invoke(definition.getService(), objects));
    }
}
