package org.kin.framework.event;

import com.google.common.base.Preconditions;
import org.kin.framework.proxy.ProxyEnhanceUtils;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.ProxyMethodDefinition;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件分发器
 * 在当前线程处理事件逻辑
 *
 * @author 健勤
 * @date 2017/8/8
 */
public class EventDispatcher implements Dispatcher, NullEventDispatcher {
    /** 存储事件与其对应的事件处理器的映射 */
    private final Map<Class<?>, ProxyInvoker<?>> event2Handler = new ConcurrentHashMap<>();

    /** 是否使用字节码增强技术 */
    private final boolean isEnhance;
    /** 字节码增强代理类包名 */
    private String proxyEnhancePackageName;

    public EventDispatcher() {
        this(false);
    }

    public EventDispatcher(boolean isEnhance) {
        this.isEnhance = isEnhance;
        if (isEnhance) {
            proxyEnhancePackageName = getClass().getPackage().getName().concat(".proxy");
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ProxyInvoker<?> getHandler(Object obj, Method method) {
        if (isEnhance) {
            return ProxyEnhanceUtils.enhanceMethod(new ProxyMethodDefinition(obj, method, proxyEnhancePackageName));
        } else {
            return new ProxyEventHandler(obj, method);
        }
    }

    @Override
    public void register(Class<?> eventClass, Object proxy, Method method) {
        if (!AbstractEvent.class.isAssignableFrom(eventClass) && !eventClass.isAnnotationPresent(Event.class)) {
            throw new IllegalArgumentException("event class must extends AbstractEvent or annotated @Event");
        }

        //event2Dispatcher需同步,防止多写的情况
        synchronized (event2Handler) {
            ProxyInvoker<?> registered = event2Handler.get(eventClass);
            if (registered == null) {
                event2Handler.put(eventClass, getHandler(proxy, method));
            } else if (!(registered instanceof MultiEventHandler)) {
                MultiEventHandler multiHandler = new MultiEventHandler();
                multiHandler.addHandler(registered);
                multiHandler.addHandler(getHandler(proxy, method));
                event2Handler.put(eventClass, multiHandler);
            } else {
                ((MultiEventHandler) registered).addHandler(getHandler(proxy, method));
            }
        }
    }

    /**
     * 分派事件
     */
    protected final void dispatch(EventContext eventContext) {
        Class<?> type = eventContext.getEvent().getClass();
        ProxyInvoker<?> handler = event2Handler.get(type);
        if (handler != null) {
            try {
                Object result = handler.invoke(eventContext.getRealParams(handler.getMethod()));
                eventContext.callback.finish(result);
            } catch (Exception e) {
                eventContext.callback.failure(e);
            }
        } else {
            throw new IllegalStateException("can not find event handler to handle event " + type);
        }
    }

    @Override
    public void dispatch(Object event, Object... params) {
        dispatch(event, EventCallback.EMPTY, params);
    }

    @Override
    public void dispatch(Object event, EventCallback callback, Object... params) {
        dispatch(new EventContext(event, params, callback));
    }

    @Override
    public void dispatch(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void shutdown() {
        event2Handler.clear();

        for (ProxyInvoker<?> proxyInvoker : event2Handler.values()) {
            if (proxyInvoker instanceof MultiEventHandler) {
                for (ProxyInvoker<?> handler : ((MultiEventHandler) proxyInvoker).handlers) {
                    detachProxyClass(handler);
                }
            } else {
                detachProxyClass(proxyInvoker);
            }
        }
    }

    /**
     * 移除无效javassist代理类
     */
    private void detachProxyClass(ProxyInvoker<?> proxyInvoker) {
        if (!(proxyInvoker instanceof ProxyEventHandler) && !(proxyInvoker instanceof MultiEventHandler)) {
            ProxyEnhanceUtils.detach(proxyInvoker.getClass().getName());
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 事件处理器的代理封装
     */
    private class ProxyEventHandler implements ProxyInvoker<Object> {
        private final Object proxy;
        private final Method method;

        public ProxyEventHandler(Object proxy, Method method) {
            this.proxy = proxy;
            this.method = method;
        }


        @Override
        public Object getProxyObj() {
            return proxy;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Object invoke(Object... params) throws Exception {
            return method.invoke(proxy, params);
        }
    }


    /**
     * 一事件对应多个事件处理器的场景
     */
    private class MultiEventHandler implements ProxyInvoker<Object> {
        private final List<ProxyInvoker<?>> handlers;

        MultiEventHandler() {
            this.handlers = new LinkedList<>();
        }

        @Override
        public Object getProxyObj() {
            return null;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public Object invoke(Object... params) throws Exception {
            for (ProxyInvoker<?> handler : handlers) {
                handler.invoke(params);
            }

            return null;
        }

        void addHandler(ProxyInvoker<?> handler) {
            handlers.add(handler);
        }
    }

    /**
     * 事件封装
     */
    protected class EventContext {
        private final int partitionId;
        private final Object event;
        private final Map<Class<?>, Object> paramsMap;
        private final EventCallback callback;

        public EventContext(int partitionId, Object event, Object[] params, EventCallback callback) {
            this.partitionId = partitionId;
            this.event = event;
            this.paramsMap = new HashMap<>();
            for (Object param : params) {
                Class<?> paramClass = param.getClass();
                Preconditions.checkArgument(!paramsMap.containsKey(paramClass), new IllegalStateException("same param type"));
                paramsMap.put(paramClass, param);
            }
            Preconditions.checkArgument(!paramsMap.containsKey(event.getClass()), new IllegalStateException("same param type"));
            paramsMap.put(event.getClass(), event);
            if (Objects.isNull(callback)) {
                callback = EventCallback.EMPTY;
            }
            this.callback = callback;
        }

        public EventContext(Object event, Object[] params, EventCallback callback) {
            this(event.hashCode(), event, params, callback);
        }

        public EventContext(Object event, Object[] params) {
            this(event.hashCode(), event, params, EventCallback.EMPTY);
        }

        /**
         * 根据具体处理方法的参数声明内容来生成对应参数数组
         */
        public Object[] getRealParams(Method method) {
            Object[] params = new Object[method.getParameterCount()];
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                Class<?> parameterType = method.getParameterTypes()[i];
                params[i] = paramsMap.get(parameterType);
            }
            return params;
        }

        //getter
        public int getPartitionId() {
            return partitionId;
        }

        public Object getEvent() {
            return event;
        }
    }
}
