package org.kin.framework.event;

import com.google.common.base.Preconditions;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.Proxys;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.OrderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件分发器
 * 在当前线程处理事件逻辑
 * 不保证事件注册的实时性
 * <p>
 * 事件类, 目前事件类最好比较native, 也就是不带泛型的, 也最好不是集合类, 数组等等
 *
 * @author 健勤
 * @date 2017/8/8
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultEventBus implements EventBus, DirectEventBus {
    private static final Logger log = LoggerFactory.getLogger(DefaultEventBus.class);
    /** key -> event class, vaklue -> event handler */
    protected final Map<Class<?>, EventHandler<?>> event2Handler = new ConcurrentHashMap<>();
    /** 是否使用字节码增强技术 */
    private final boolean isEnhance;

    public DefaultEventBus() {
        this(true);
    }

    public DefaultEventBus(boolean isEnhance) {
        this.isEnhance = isEnhance;
    }

    /**
     * 从{@link EventHandler}泛型中解析出事件raw type
     *
     * @param handleClass {@link EventHandler} class
     */
    protected Class<?> parseEventRawTypeFromHanlder(Class<?> handleClass) {
        return parseEventRawType(ClassUtils.getSuperInterfacesGenericActualTypes(EventHandler.class, handleClass).get(0));
    }

    /**
     * 从event class 泛型实际类型中解析出事件raw type
     *
     * @param eventActualType event class 泛型实际类型
     */
    protected Class<?> parseEventRawType(Type eventActualType) {
        if (eventActualType instanceof ParameterizedType) {
            ParameterizedType parameterType = (ParameterizedType) eventActualType;
            Class<?> parameterRawType = (Class<?>) parameterType.getRawType();
            if (Collection.class.isAssignableFrom(parameterRawType)) {
                //事件合并, 获取第一个泛型参数真实类型
                //以真实事件类型来注册事件处理器
                return (Class<?>) parameterType.getActualTypeArguments()[0];
            } else {
                //普通事件
                return parameterRawType;
            }
        } else {
            return (Class<?>) eventActualType;
        }
    }

    @Override
    public void register(Object obj) {
        Preconditions.checkNotNull(obj, "arg 'obj' must be not null");

        if (obj instanceof EventHandler) {
            registerEventHandler((EventHandler) obj);
        } else {
            parseRegisterEventFunc(obj);
        }
    }

    /**
     * 从{@link EventHandler}实现解析出event class并注册
     */
    private void registerEventHandler(EventHandler eventHandler) {
        Class<?> eventClass = parseEventRawTypeFromHanlder(eventHandler.getClass());

        registerEventHandler(eventClass, eventHandler);
    }

    /**
     * 注册event class及其对应的{@link EventHandler}实现
     */
    private void registerEventHandler(Class<?> eventClass, EventHandler eventHandler) {
        EventHandler<?> registered = event2Handler.get(eventClass);
        if (registered == null) {
            event2Handler.put(eventClass, eventHandler);
        } else if (!(registered instanceof MultiEventHandlers)) {
            MultiEventHandlers multiHandler = new MultiEventHandlers();
            multiHandler.addHandler(registered);
            multiHandler.addHandler(eventHandler);
            event2Handler.put(eventClass, multiHandler);
        } else {
            ((MultiEventHandlers) registered).addHandler(eventHandler);
        }
    }

    /**
     * 解析出带{@link EventFunction}方法并进行注册
     */
    private void parseRegisterEventFunc(Object obj) {
        Class<?> claxx = obj.getClass();

        //注解在方法
        //在所有  public & 有注解的  方法中寻找一个匹配的方法作为事件处理方法
        for (Method method : claxx.getMethods()) {
            if (method.isAnnotationPresent(EventFunction.class)) {
                Type[] parameterTypes = method.getGenericParameterTypes();
                int paramLen = parameterTypes.length;
                if (paramLen <= 0 || paramLen > 2) {
                    //只处理一个或两个参数的public方法
                    continue;
                }

                Class<?> eventClass = null;
                //EventBus实现类的方法参数位置, 默认没有
                int busParamIndex = 0;
                for (int i = 1; i <= parameterTypes.length; i++) {
                    if (parameterTypes[i - 1] instanceof ParameterizedType) {
                        ParameterizedType parameterType = (ParameterizedType) parameterTypes[i - 1];
                        Class<?> parameterRawType = (Class<?>) parameterType.getRawType();
                        if (EventBus.class.isAssignableFrom(parameterRawType)) {
                            busParamIndex = i;
                        } else {
                            eventClass = parseEventRawType(parameterType);
                        }
                    } else {
                        //普通事件
                        Class<?> parameterType = (Class<?>) parameterTypes[i - 1];
                        if (EventBus.class.isAssignableFrom(parameterType)) {
                            busParamIndex = i;
                        } else {
                            eventClass = parseEventRawType(parameterType);
                        }
                    }
                }

                registerEventFunc(eventClass, obj, method, busParamIndex);
            }
        }
    }

    /**
     * 注册基于{@link EventFunction}注入的事件处理方法
     *
     * @param busParamIndex {@link EventBus}实现类的方法参数位置, 默认0, 标识没有该参数
     */
    private void registerEventFunc(Class<?> eventClass, Object proxy, Method method, int busParamIndex) {
        Order order = method.getAnnotation(Order.class);
        registerEventHandler(eventClass,
                new MethodBaseEventHandler<>(
                        getMethodProxy(proxy, method),
                        busParamIndex,
                        OrderUtils.getOrder(method,
                                Objects.nonNull(order) ? order.value() : Ordered.LOWEST_PRECEDENCE)));
    }

    /**
     * @return 带 {@link EventFunction} 方法代理类
     */
    private ProxyInvoker<?> getMethodProxy(Object obj, Method method) {
        MethodDefinition<Object> methodDefinition = new MethodDefinition<>(obj, method);
        if (isEnhance) {
            return Proxys.byteBuddy().enhanceMethod(methodDefinition);
        } else {
            return Proxys.reflection().enhanceMethod(methodDefinition);
        }
    }

    /**
     * 分派事件逻辑
     */
    protected void doPost(EventContext eventContext) {
        Object event = eventContext.getEvent();
        doPost(event.getClass(), event);
    }

    /**
     * 分派事件逻辑
     *
     * @param eventClass 事件类型
     * @param event      事件实例
     */
    protected final void doPost(Class<?> eventClass, Object event) {
        EventHandler handler = event2Handler.get(eventClass);
        if (handler != null) {
            try {
                handler.handle(this, event);
            } catch (Exception e) {
                log.error("", e);
            }
        } else {
            throw new IllegalStateException("can not find event handler to handle event " + eventClass);
        }
    }

    @Override
    public void post(Object event) {
        doPost(new EventContext(event));
    }

    @Override
    public final void post(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void shutdown() {
        if (isEnhance) {
            List<Class<?>> enhanceClasses = new ArrayList<>(event2Handler.size());
            try {
                for (EventHandler eventHandler : event2Handler.values()) {
                    if (eventHandler instanceof MultiEventHandlers) {
                        List<EventHandler> handlers = ((MultiEventHandlers) eventHandler).getHandlers();
                        for (EventHandler eventHandler1 : handlers) {
                            if (eventHandler1 instanceof MethodBaseEventHandler) {
                                enhanceClasses.add(eventHandler.getClass());
                            }
                        }
                    } else if (eventHandler instanceof MethodBaseEventHandler) {
                        enhanceClasses.add(eventHandler.getClass());
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }

        event2Handler.clear();
    }
}
