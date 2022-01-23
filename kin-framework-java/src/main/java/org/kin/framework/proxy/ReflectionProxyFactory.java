package org.kin.framework.proxy;

import org.kin.framework.utils.SysUtils;

import java.lang.reflect.Proxy;

/**
 * 基于反射 proxy
 *
 * @author huangjianqin
 * @date 2020/12/24
 */
public final class ReflectionProxyFactory implements ProxyFactory {
    /** 单例 */
    public static final ReflectionProxyFactory INSTANCE = new ReflectionProxyFactory();

    private ReflectionProxyFactory() {
    }

    @Override
    public <T> ProxyInvoker<T> enhanceMethod(MethodDefinition<T> definition) {
        return new ReflectionInvoker<>(definition.getService(), definition.getMethod());
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
