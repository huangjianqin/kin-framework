package org.kin.framework.event.impl;

import org.kin.framework.event.annotation.Listener;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简单的监听器实现
 *
 * @author huangjianqin
 * @date 2019/3/1
 */
public class SimpleListenerManager {
    private static SimpleListenerManager DEFALUT;

    private Map<Class<?>, List<ListenerDetail>> listeners = new HashMap<>();

    public static SimpleListenerManager instance() {
        if (DEFALUT == null) {
            synchronized (SimpleListenerManager.class) {
                if (DEFALUT == null) {
                    DEFALUT = new SimpleListenerManager();
                }
            }
        }

        return DEFALUT;
    }

    //------------------------------------------------------------------------------------------------------------------

    private class ListenerDetail {
        private Object instance;
        private int order;

        public ListenerDetail(Object instance, int order) {
            this.instance = instance;
            this.order = order;
        }

        //getter
        public Object getInstance() {
            return instance;
        }

        public int getOrder() {
            return order;
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    protected void register0(Object o) {
        Class claxx = o.getClass();
        while (claxx != null) {
            for (Class interfaceClass : claxx.getInterfaces()) {
                if (interfaceClass.isAnnotationPresent(Listener.class)) {
                    List<ListenerDetail> list = listeners.get(interfaceClass);
                    if (list == null) {
                        list = new ArrayList<>();
                    } else {
                        list = new ArrayList<>(list);
                    }

                    int order = ((Listener) interfaceClass.getAnnotation(Listener.class)).order();
                    ListenerDetail listenerDetail = new ListenerDetail(o, order);

                    list.add(listenerDetail);
                    listeners.put(interfaceClass, list);
                }
            }
            claxx = claxx.getSuperclass();
        }
    }

    public synchronized void register(Object bean) {
        register0(bean);
        sortAll();
    }

    private void sort(Map<Class<?>, List<ListenerDetail>> listeners, Class<?> key) {
        List<ListenerDetail> list = listeners.get(key);
        if (list != null && !list.isEmpty()) {
            list = new ArrayList<>(list);
            list.sort(Comparator.comparingInt(ListenerDetail::getOrder));
            listeners.put(key, list);
        }
    }

    protected synchronized void sortAll() {
        //排序
        Map<Class<?>, List<ListenerDetail>> listeners = new HashMap<>(this.listeners);
        for (Class<?> key : listeners.keySet()) {
            sort(listeners, key);
        }
        this.listeners = listeners;
    }

    public <T> List<T> getListener(Class<T> listenerClass) {
        return (List<T>) listeners.getOrDefault(listenerClass, Collections.emptyList())
                .stream().map(ListenerDetail::getInstance).collect(Collectors.toList());
    }

    //------------------------------------------------------------------------------------------------------------------
}
