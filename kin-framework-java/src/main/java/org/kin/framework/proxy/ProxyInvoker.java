package org.kin.framework.proxy;

import java.lang.reflect.Method;

/**
 * 方法代理类接口
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public interface ProxyInvoker<S> {
    /**
     * @return 实现类
     */
    S getProxyObj();

    /**
     * @return 目标方法
     */
    Method getMethod();

    /**
     * 调用
     *
     * @param params 参数
     * @return 调用方法返回结果
     */
    Object invoke(Object... params) throws Exception;
}
