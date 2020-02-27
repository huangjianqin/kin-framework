package org.kin.framework.event.impl;

import com.google.common.base.Preconditions;
import org.kin.framework.concurrent.PartitionTaskExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.concurrent.impl.EfficientHashPartitioner;
import org.kin.framework.event.AbstractEvent;
import org.kin.framework.event.EventCallback;
import org.kin.framework.event.NullEventDispatcher;
import org.kin.framework.event.ScheduleDispatcher;
import org.kin.framework.event.annotation.Event;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.ProxyMethodDefinition;
import org.kin.framework.proxy.utils.ProxyEnhanceUtils;
import org.kin.framework.service.AbstractService;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author 健勤
 * @date 2017/8/8
 * 事件分发器
 * 支持多线程事件处理
 */
public class EventDispatcher extends AbstractService implements ScheduleDispatcher, NullEventDispatcher {
    private static Logger log = LoggerFactory.getLogger(EventDispatcher.class);
    /** 事件堆积太多阈值 */
    private static final int TOO_MUCH_EVENTS_THRESHOLD = 1000;

    /** 事件处理线程(分区处理) */
    protected final PartitionTaskExecutor<Integer> executor;
    /** 负责分发事件的线程(主要负责调度和异步分发事件) */
    protected final ThreadManager threadManager;
    /** 异步分发事件线程逻辑实现 */
    protected AsyncEventDispatchThread asyncDispatchThread;
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
        super("EventDispatcher");
        executor = new PartitionTaskExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE);
        event2Handler = new HashMap<>();

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                SysUtils.getSuitableThreadNum(),
                SysUtils.getSuitableThreadNum(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new SimpleThreadFactory("EventDispatcher$event-handler-"));
        threadManager = new ThreadManager(pool, SysUtils.getSuitableThreadNum() / 2 + 2,
                new SimpleThreadFactory("EventDispatcher$schedule-event-handler-"));
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
                    eventContext.callback.exception(e);
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
    public void asyncDispatch(Object event, Object... params) {
        asyncDispatch(event, EventCallback.EMPTY, params);
    }

    @Override
    public void asyncDispatch(Object event, EventCallback callback, Object... params) {
        asyncDispatchThread.handleEvent(new EventContext(event, params, callback));
    }

    @Override
    public Future<?> scheduleDispatch(Object event, TimeUnit unit, long delay, Object... params) {
        return threadManager.schedule(() -> dispatch(event, params), delay, unit);
    }

    @Override
    public Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period, Object... params) {
        return threadManager.scheduleAtFixedRate(() -> dispatch(event, params), initialDelay, period, unit);
    }

    @Override
    public void dispatch(Runnable runnable) {
        executor.execute(runnable.hashCode(), runnable);
    }

    @Override
    public void serviceInit() {
    }

    @Override
    public void serviceStart() {
        asyncDispatchThread = new AsyncEventDispatchThread();
        threadManager.execute(asyncDispatchThread);
    }

    @Override
    public void serviceStop() {
        asyncDispatchThread.shutdown();
        executor.shutdown();
        threadManager.shutdown();
        event2Handler.clear();
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 事件处理线程,主要逻辑是从事件队列获得事件并分派出去
     */
    protected final class AsyncEventDispatchThread implements Runnable {
        private volatile boolean isStopped = false;
        private BlockingQueue<EventContext> eventContextQueue = new LinkedBlockingQueue<>();
        private volatile Thread bind = null;

        private void dispatchEvent(EventContext one) {
            do {
                if (one != null) {
                    try {
                        dispatch(one);
                    } catch (Exception e) {
                        ExceptionUtils.log(e);
                    }
                }
            } while ((one = eventContextQueue.poll()) != null);
        }

        @Override
        public void run() {
            try {
                bind = Thread.currentThread();
                while (!isStopped && !Thread.currentThread().isInterrupted()) {
                    try {
                        EventContext eventContext = eventContextQueue.take();
                        dispatchEvent(eventContext);
                    } catch (InterruptedException e) {
                    }
                }
            } finally {
                //尽可能处理完队列里面的事件
                dispatchEvent(null);
            }
        }

        public void handleEvent(EventContext eventContext) {
            if (!isStopped) {
                //用于统计并打印相关信息日志
                int remCapacity = eventContextQueue.remainingCapacity();
                if (remCapacity > TOO_MUCH_EVENTS_THRESHOLD) {
                    log.warn("high remaining capacity in the event-queue: " + remCapacity);
                }

                try {
                    eventContextQueue.put(eventContext);
                } catch (InterruptedException e) {
                }
            }
        }

        void shutdown() {
            isStopped = true;
            bind.interrupt();
        }
    }

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
    protected class EventContext {
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

        public EventCallback getCallback() {
            return callback;
        }
    }
}
