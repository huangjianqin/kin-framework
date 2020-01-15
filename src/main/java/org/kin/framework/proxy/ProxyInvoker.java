package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 利用javassist字节码技术增加方法的代理类接口
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ProxyInvoker<S> {
    /**
     * 获取代理类
     */
    S getProxyObj();

    /**
     * 获取方法
     */
    Method getMethod();

    /**
     * 调用
     */
    Object invoke(Object... params) throws Exception;
}
