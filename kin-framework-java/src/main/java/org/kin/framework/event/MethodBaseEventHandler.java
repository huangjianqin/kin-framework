package org.kin.framework.event;

import org.kin.framework.common.Ordered;
import org.kin.framework.proxy.ProxyInvoker;

/**
 * 基于{@link EventFunction}注解的方法的{@link EventHandler}
 *
 * @author huangjianqin
 * @date 2021/3/12
 */
class MethodBaseEventHandler<T> implements EventHandler<T>, Ordered {
    /** 事件处理方法代理 */
    private final ProxyInvoker<?> proxy;
    /** EventDispatcher实现类的方法参数位置, 默认没有 */
    private final int dispatcherParamIndex;
    /** 优先级 */
    private final int order;

    public MethodBaseEventHandler(ProxyInvoker<?> proxy, int dispatcherParamIndex) {
        this(proxy, dispatcherParamIndex, LOWEST_PRECEDENCE);
    }

    MethodBaseEventHandler(ProxyInvoker<?> proxy, int dispatcherParamIndex, int order) {
        this.proxy = proxy;
        this.dispatcherParamIndex = dispatcherParamIndex;
        this.order = order;
    }

    @Override
    public void handle(EventDispatcher dispatcher, T event) throws Exception {
        Object[] params;
        if (dispatcherParamIndex == 1) {
            params = new Object[]{dispatcher, event};
        } else if (dispatcherParamIndex == 2) {
            params = new Object[]{event, dispatcher};
        } else {
            params = new Object[]{event};
        }
        proxy.invoke(params);
    }

    @Override
    public int getOrder() {
        return order;
    }

    //getter
    ProxyInvoker<?> getProxy() {
        return proxy;
    }
}
