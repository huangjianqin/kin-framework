package org.kin.framework.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * 获取有{@link EventFunction}注解或者实现了{@link EventHandler}的bean并注册事件及其处理器
 *
 * @author huangjianqin
 * @date 2019/3/1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SpringEventBus extends DefaultOrderedEventBus implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(SpringEventBus.class);

    public SpringEventBus(int parallelism, boolean isEnhance) {
        super(parallelism, isEnhance);
    }

    public SpringEventBus(int parallelism) {
        super(parallelism);
    }

    /**
     * 识别带{@link EventFunction}注解的public 方法, 并自动注册
     */
    private void registerAnnoBaseEventHandler(ApplicationContext applicationContext) {
        //处理带有HandleEvent注解的方法
        Map<String, Object> beansWithAnno = applicationContext.getBeansWithAnnotation(HandleEvent.class);
        for (Object bean : beansWithAnno.values()) {
            register(bean);
        }

        //处理EventHandler bean
        Map<String, EventHandler> eventHandlerBeans = applicationContext.getBeansOfType(EventHandler.class);
        for (EventHandler eventHandler : eventHandlerBeans.values()) {
            register(eventHandler);
        }
    }

    //------------------------------------------------------------------------------------------------------------------

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        registerAnnoBaseEventHandler(event.getApplicationContext());
    }
}
