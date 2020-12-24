package org.kin.framework.proxy;

/**
 * 代理类增强工厂
 *
 * @author huangjianqin
 * @date 2020/12/23
 */
interface ProxyFactory {
    /**
     * 增强代理方法
     */
    @SuppressWarnings("unchecked")
    <T> ProxyInvoker<T> enhanceMethod(MethodDefinition<T> definition);

    /**
     * 增强代理类(接口)
     */
    @SuppressWarnings("unchecked")
    <P> P enhanceClass(ClassDefinition<P> definition);
}
