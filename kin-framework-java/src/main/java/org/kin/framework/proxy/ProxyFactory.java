package org.kin.framework.proxy;

/**
 * 代理类增强工厂
 *
 * @author huangjianqin
 * @date 2020/12/23
 */
public interface ProxyFactory {
    /**
     * 增强代理方法
     */
    <T> ProxyInvoker<T> enhanceMethod(MethodDefinition<T> definition);

    /**
     * 增强代理类(接口)
     */
    <P> P enhanceClass(ClassDefinition<P> definition);
}
