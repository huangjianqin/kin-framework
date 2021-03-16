package org.kin.framework.event;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件分发器
 * 在当前线程处理事件逻辑
 * 不保证事件注册的实时性
 *
 * @author 健勤
 * @date 2017/8/8
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultEventDispatcher implements EventDispatcher, NullEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(DefaultEventDispatcher.class);
    /** key -> event class, vaklue -> event handler */
    protected final Map<Class<?>, EventHandler<?>> event2Handler = new ConcurrentHashMap<>();

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
    public <T> void register(Class<T> eventClass, EventHandler<T> eventHandler) {
        Preconditions.checkNotNull(eventClass, "event class must be not null");
        Preconditions.checkNotNull(eventHandler, "event handler must be not null");

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
     * 分派事件逻辑
     */
    protected void doDispatch(EventContext eventContext) {
        Object event = eventContext.getEvent();
        doDispatch(event.getClass(), event);
    }

    /**
     * 分派事件逻辑
     *
     * @param eventClass 事件类型
     * @param event      事件实例
     */
    protected final void doDispatch(Class<?> eventClass, Object event) {
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
    public void dispatch(Object event) {
        doDispatch(new EventContext(event));
    }

    @Override
    public void dispatch(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void shutdown() {
        event2Handler.clear();
    }
}
