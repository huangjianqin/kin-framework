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
    /** {@link EventBus} 实现类的方法参数位置, 默认没有 */
    private final int busParamIndex;
    /** 优先级 */
    private final int order;

    public MethodBaseEventHandler(ProxyInvoker<?> proxy, int busParamIndex) {
        this(proxy, busParamIndex, LOWEST_PRECEDENCE);
    }

    MethodBaseEventHandler(ProxyInvoker<?> proxy, int busParamIndex, int order) {
        this.proxy = proxy;
        this.busParamIndex = busParamIndex;
        this.order = order;
    }

    @Override
    public void handle(EventBus bus, T event) throws Exception {
        Object[] params;
        if (busParamIndex == 1) {
            params = new Object[]{bus, event};
        } else if (busParamIndex == 2) {
            params = new Object[]{event, bus};
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
