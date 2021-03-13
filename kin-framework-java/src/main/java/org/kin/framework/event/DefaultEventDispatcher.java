package org.kin.framework.event;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 真正分派事件逻辑
     */
    protected void realDispatch(EventContext eventContext) {
        Object event = eventContext.getEvent();
        realDispatch0(event.getClass(), event);
    }

    /**
     * 真正分派事件逻辑
     *
     * @param eventClass 事件类型
     * @param event      事件实例
     */
    protected final void realDispatch0(Class<?> eventClass, Object event) {
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
        realDispatch(new EventContext(event));
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
