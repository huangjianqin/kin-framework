package org.kin.framework.event;

import org.kin.framework.proxy.Javassists;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.Proxys;
import org.kin.framework.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 获取有{@link EventFunction}注解或者实现了{@link EventHandler}的bean并注册事件及其处理器
 *
 * @author huangjianqin
 * @date 2019/3/1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SpringEventDispatcher extends DefaultOrderedEventDispatcher implements ApplicationListener<ApplicationEvent> {
    private static final Logger log = LoggerFactory.getLogger(SpringEventDispatcher.class);
    /** 是否使用字节码增强技术 */
    private final boolean isEnhance;

    public SpringEventDispatcher(int parallelism, boolean isEnhance) {
        super(parallelism);
        this.isEnhance = isEnhance;
    }

    /**
     * @return 方法代理类
     */
    private ProxyInvoker<?> getHandler(Object obj, Method method) {
        MethodDefinition<Object> methodDefinition = new MethodDefinition<>(obj, method);
        if (isEnhance) {
            return Proxys.byteBuddy().enhanceMethod(methodDefinition);
        } else {
            return Proxys.reflection().enhanceMethod(methodDefinition);
        }
    }

    /**
     * 注册基于{@link EventFunction}注入的事件处理方法
     *
     * @param dispatcherParamIndex EventDispatcher实现类的方法参数位置, 默认没有
     */
    private void register(Class<?> eventClass, Object proxy, Method method, int dispatcherParamIndex) {
        register(eventClass, new MethodBaseEventHandler<>(getHandler(proxy, method), dispatcherParamIndex));
    }

    @Override
    public void shutdown() {
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

        super.shutdown();

        if (isEnhance && CollectionUtils.isNonEmpty(enhanceClasses)) {
            for (Class<?> enhanceClass : enhanceClasses) {
                Javassists.detach(enhanceClass.getName());
            }
        }
    }

    /**
     * 识别带{@link EventFunction}注解的public 方法, 并自动注册
     */
    private void registerAnnoBaseEventHandler(ApplicationContext applicationContext) {
        //处理带有HandleEvent注解的方法
        Map<String, Object> beansWithAnno = applicationContext.getBeansWithAnnotation(HandleEvent.class);
        for (Object bean : beansWithAnno.values()) {
            Class<?> claxx = bean.getClass();

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
                    //EventDispatcher实现类的方法参数位置, 默认没有
                    int dispatcherParamIndex = 0;
                    for (int i = 1; i <= parameterTypes.length; i++) {
                        if (parameterTypes[i - 1] instanceof ParameterizedType) {
                            ParameterizedType parameterType = (ParameterizedType) parameterTypes[i - 1];
                            Class<?> parameterRawType = (Class<?>) parameterType.getRawType();
                            if (EventDispatcher.class.isAssignableFrom(parameterRawType)) {
                                dispatcherParamIndex = i;
                            } else {
                                eventClass = parseEventRawType(parameterType);
                            }
                        } else {
                            //普通事件
                            Class<?> parameterType = (Class<?>) parameterTypes[i - 1];
                            if (EventDispatcher.class.isAssignableFrom(parameterType)) {
                                dispatcherParamIndex = i;
                            } else {
                                eventClass = parseEventRawType(parameterType);
                            }
                        }
                    }

                    register(eventClass, bean, method, dispatcherParamIndex);
                }
            }
        }

        //处理EventHandler bean
        Map<String, EventHandler> eventHandlerBeans = applicationContext.getBeansOfType(EventHandler.class);
        for (EventHandler eventHandler : eventHandlerBeans.values()) {
            register(parseEventRawTypeFromHanlder(eventHandler.getClass()), eventHandler);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextRefreshedEvent) {
            registerAnnoBaseEventHandler(((ContextRefreshedEvent) applicationEvent).getApplicationContext());
        }

        if (applicationEvent instanceof ContextClosedEvent) {
            shutdown();
        }
    }
}
