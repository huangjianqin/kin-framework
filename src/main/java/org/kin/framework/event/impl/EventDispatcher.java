package org.kin.framework.event.impl;

import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.PartitionTaskExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.impl.EfficientHashPartitioner;
import org.kin.framework.event.AbstractEvent;
import org.kin.framework.event.EventCallback;
import org.kin.framework.event.NullEventDispatcher;
import org.kin.framework.event.ScheduleDispatcher;
import org.kin.framework.event.annotation.Event;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.ProxyMethodDefinition;
import org.kin.framework.proxy.utils.ProxyEnhanceUtils;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 健勤
 * @date 2017/8/8
 * 事件分发器
 * 支持多线程事件处理
 */
public class EventDispatcher implements ScheduleDispatcher, NullEventDispatcher {
    private static Logger log = LoggerFactory.getLogger(EventDispatcher.class);

    /** 事件处理线程(分区处理) */
    protected final PartitionTaskExecutor<Integer> executor;
    /** 调度线程 */
    protected final ScheduledExecutorService scheduledExecutors;
    /** 存储事件与其对应的事件处理器的映射 */
    protected final Map<Class<?>, ProxyInvoker> event2Handler;
    /** 是否使用字节码增强技术 */
    private final boolean isEnhance;
    /** 字节码增强代理类包名 */
    private String proxyEnhancePackageName;

    public EventDispatcher(int parallelism) {
        this(parallelism, false);
    }

    public EventDispatcher(int parallelism, boolean isEnhance) {
        executor = new PartitionTaskExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE, "EventDispatcher$event-handler-");
        event2Handler = new HashMap<>();

        scheduledExecutors = new ScheduledThreadPoolExecutor(SysUtils.getSuitableThreadNum() / 2 + 1,
                new SimpleThreadFactory("EventDispatcher$schedule-event-"));
        this.isEnhance = isEnhance;
        if (isEnhance) {
            proxyEnhancePackageName = "org.kin.framework.event.handler.proxy";
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    private ProxyInvoker getHandler(Object obj, Method method) {
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
            ProxyInvoker registered = event2Handler.get(eventClass);
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

    protected void dispatch(EventContext eventContext) {
        Class<?> type = eventContext.getEvent().getClass();
        ProxyInvoker handler = event2Handler.get(type);
        if (handler != null) {
            executor.execute(eventContext.getPartitionId(), () -> {
                try {
                    Object result = handler.invoke(eventContext.getRealParams(handler.getMethod()));
                    eventContext.callback.finish(result);
                } catch (Exception e) {
                    eventContext.callback.failure(e);
                }
            });
        } else {
            throw new IllegalStateException("doesn't have event handler to handle event " + type);
        }
    }

    @Override
    public void dispatch(Object event, Object... params) {
        dispatch(event, EventCallback.EMPTY, params);
    }

    @Override
    public void dispatch(Object event, EventCallback callback, Object... params) {
        dispatch(new EventContext(event.hashCode(), event, params, callback));
    }

    @Override
    public Future<?> scheduleDispatch(Object event, TimeUnit unit, long delay, Object... params) {
        return scheduledExecutors.schedule(() -> dispatch(event, params), delay, unit);
    }

    @Override
    public Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period, Object... params) {
        return scheduledExecutors.scheduleAtFixedRate(() -> dispatch(event, params), initialDelay, period, unit);
    }

    @Override
    public void dispatch(Runnable runnable) {
        executor.execute(runnable.hashCode(), runnable);
    }

    public void shutdown() {
        executor.shutdown();
        scheduledExecutors.shutdown();
        event2Handler.clear();
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 事件处理器的代理封装
     */
    private class ProxyEventHandler implements ProxyInvoker {
        private Object proxy;
        private Method method;

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
    private class MultiEventHandler implements ProxyInvoker {
        private List<ProxyInvoker> handlers;

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
        public Object invoke(Object... params) {
            for (ProxyInvoker handler : handlers) {
                try {
                    handler.invoke(params);
                } catch (Exception e) {
                    log.error("", e);
                }
            }

            return null;
        }

        void addHandler(ProxyInvoker handler) {
            handlers.add(handler);
        }
    }

    /**
     * 事件封装
     */
    private class EventContext {
        private int partitionId;
        private Object event;
        private Map<Class<?>, Object> paramsMap;
        private EventCallback callback;

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
