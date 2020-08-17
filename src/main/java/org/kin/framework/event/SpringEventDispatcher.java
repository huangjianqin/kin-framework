package org.kin.framework.event;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.lang.NonNull;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 获取注释有@HandleEvent的bean并注册事件及其处理器
 *
 * @author huangjianqin
 * @date 2019/3/1
 */
public class SpringEventDispatcher extends EventDispatcher implements ApplicationContextAware, ApplicationListener {
    public SpringEventDispatcher(int parallelism) {
        super(parallelism);
    }

    public SpringEventDispatcher(int parallelism, boolean isEnhance) {
        super(parallelism, isEnhance);
    }

    //------------------------------------------------------------------------------------------------------------------

    /**
     * 识别@HandleEvent注解的类 or 方法, 并自动注册
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(HandleEvent.class);
        for (Object bean : beans.values()) {
            Class claxx = bean.getClass();
            //如果注解在类声明的话, 默认全部public方法都需要检查是否满足注册事件的要求
            //否则, 只检查有@HandleEvent注解的方法
            boolean isClassWithAnno = claxx.isAnnotationPresent(HandleEvent.class);

            //注解在方法
            //在所有  public & 有注解的  方法中寻找一个匹配的方法作为事件处理方法
            for (Method method : claxx.getMethods()) {
                if (isClassWithAnno || method.isAnnotationPresent(HandleEvent.class)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != 1) {
                        //只有一个参数才处理
                        continue;
                    }
                    for (Class<?> parameterClass : parameterTypes) {
                        if (AbstractEvent.class.isAssignableFrom(parameterClass) ||
                                parameterClass.isAnnotationPresent(Event.class)) {
                            //只要一个参数继承了Event, 则注册
                            register(parameterClass, bean, method);
                        }
                    }
                }
            }
        }
    }

    @Override
    @NonNull
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ContextClosedEvent) {
            shutdown();
        }
    }
}
