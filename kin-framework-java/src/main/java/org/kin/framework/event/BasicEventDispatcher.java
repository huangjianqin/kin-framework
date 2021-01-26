package org.kin.framework.event;

import org.kin.framework.proxy.Javassists;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.Proxys;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件分发器
 * 在当前线程处理事件逻辑
 * 不保证动态事件注册线程安全
 *
 * @author 健勤
 * @date 2017/8/8
 */
public class BasicEventDispatcher implements EventDispatcher, NullEventDispatcher {
    /** 存储事件与其对应的事件处理器的映射 */
    private final Map<Class<?>, ProxyInvoker<?>> event2Handler = new ConcurrentHashMap<>();
    /** 是否使用字节码增强技术 */
    private final boolean isEnhance;

    public BasicEventDispatcher() {
        this(false);
    }

    public BasicEventDispatcher(boolean isEnhance) {
        this.isEnhance = isEnhance;
    }

    //------------------------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private ProxyInvoker<?> getHandler(Object obj, Method method) {
        MethodDefinition<Object> methodDefinition = new MethodDefinition<>(obj, method);
        if (isEnhance) {
            return Proxys.javassist().enhanceMethod(methodDefinition);
        } else {
            return Proxys.reflection().enhanceMethod(methodDefinition);
        }
    }

    @Override
    public final void register(Class<?> eventClass, Object proxy, Method method) {
        if (!AbstractEvent.class.isAssignableFrom(eventClass) && !eventClass.isAnnotationPresent(Event.class)) {
            throw new IllegalArgumentException("event class must extends AbstractEvent or annotated @Event");
        }

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

    /**
     * 分派事件
     */
    protected final void dispatch(EventContext eventContext) {
        Class<?> type = eventContext.getEvent().getClass();
        ProxyInvoker<?> handler = event2Handler.get(type);
        if (handler != null) {
            try {
                Object result = handler.invoke(eventContext.getEvent());
                eventContext.callback.finish(result);
            } catch (Exception e) {
                eventContext.callback.failure(e);
            }
        } else {
            throw new IllegalStateException("can not find event handler to handle event " + type);
        }
    }

    @Override
    public void dispatch(Object event) {
        dispatch(event, EventCallback.EMPTY);
    }

    @Override
    public void dispatch(Object event, EventCallback callback) {
        dispatch(new EventContext(event, callback));
    }

    @Override
    public void dispatch(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void shutdown() {
        for (ProxyInvoker<?> proxyInvoker : event2Handler.values()) {
            if (proxyInvoker instanceof MultiEventHandler) {
                for (ProxyInvoker<?> handler : ((MultiEventHandler) proxyInvoker).handlers) {
                    detachProxyClass(handler);
                }
            } else {
                detachProxyClass(proxyInvoker);
            }
        }

        event2Handler.clear();
    }

    /**
     * 移除无效javassist代理类
     */
    private void detachProxyClass(ProxyInvoker<?> proxyInvoker) {
        if (isEnhance && !(proxyInvoker instanceof MultiEventHandler)) {
            Javassists.detach(proxyInvoker.getClass().getName());
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 一事件对应多个事件处理器的场景
     */
    private static class MultiEventHandler implements ProxyInvoker<Object> {
        /** 事件处理器列表 */
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
    static class EventContext {
        /** 分区id */
        private final int partitionId;
        /** 事件 */
        private final Object event;
        /** 事件处理回调 */
        private final EventCallback callback;

        public EventContext(int partitionId, Object event, EventCallback callback) {
            this.partitionId = partitionId;
            this.event = event;
            if (Objects.isNull(callback)) {
                callback = EventCallback.EMPTY;
            }
            this.callback = callback;
        }

        public EventContext(Object event, EventCallback callback) {
            this(event.hashCode(), event, callback);
        }

        public EventContext(Object event) {
            this(event.hashCode(), event, EventCallback.EMPTY);
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
