package org.kin.framework.event;

import org.kin.framework.utils.OrderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * 用于支持一个event, 对应多个event handler的场景
 *
 * @author huangjianqin
 * @date 2021/3/12
 */
class MultiEventHandlers<T> implements EventHandler<T> {
    private static final Logger log = LoggerFactory.getLogger(MultiEventHandlers.class);

    /** 事件处理器集合 */
    private final List<EventHandler<T>> handlers = new LinkedList<>();

    @Override
    public void handle(EventBus bus, T event) throws Exception {
        for (EventHandler<T> handler : handlers) {
            try {
                handler.handle(bus, event);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    void addHandler(EventHandler<T> handler) {
        handlers.add(handler);
        OrderUtils.sort(handlers);
    }

    //getter
    List<EventHandler<T>> getHandlers() {
        return handlers;
    }
}
