package org.kin.framework.event.impl;

import org.kin.framework.event.Listener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by huangjianqin on 2019/3/1.
 */
//不想引用该jar并使用spring时, 自动加载项目不使用的bean. 想用的话, 继承并使用@Component
//@Component
public class SimpleListenerManager implements ApplicationContextAware {
    private static SimpleListenerManager DEFALUT;

    private Map<Class<?>, List<ListenerDetail>> listeners = new HashMap<>();

    public static SimpleListenerManager instance() {
        if(DEFALUT == null){
            synchronized (SimpleListenerManager.class){
                if(DEFALUT == null){
                    DEFALUT = new SimpleListenerManager();
                }
            }
        }

        return DEFALUT;
    }

    //------------------------------------------------------------------------------------------------------------------
    private class ListenerDetail{
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
    private void register0(Object o) {
        Class claxx = o.getClass();
        while (claxx != null) {
            for (Class interfaceClass : claxx.getInterfaces()) {
                if (interfaceClass.isAnnotationPresent(Listener.class)) {
                    List<ListenerDetail> list = listeners.get(interfaceClass);
                    if(list == null){
                        list = new ArrayList<>();
                    }
                    else{
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
            list.sort(Comparator.comparingInt(o -> o.getOrder()));
            listeners.put(key, list);
        }
    }

    private synchronized void sortAll() {
        //排序
        Map<Class<?>, List<ListenerDetail>> listeners = new HashMap<>(this.listeners);
        for (Class<?> key : listeners.keySet()) {
            sort(listeners, key);
        }
        this.listeners = listeners;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Listener.class);
        for (Object bean : beans.values()) {
            register0(bean);
        }

        sortAll();
    }

    public <T> List<T> getListener(Class<T> listenerClass) {
        return (List<T>) listeners.getOrDefault(listenerClass, Collections.emptyList())
                .stream().map(ListenerDetail::getInstance).collect(Collectors.toList());
    }

    //------------------------------------------------------------------------------------------------------------------
}
