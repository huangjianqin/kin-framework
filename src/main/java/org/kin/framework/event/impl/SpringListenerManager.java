package org.kin.framework.event.impl;

import org.kin.framework.event.annotation.Listener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2019/7/19
 */
@Component
public class SpringListenerManager extends SimpleListenerManager implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Listener.class);
        for (Object bean : beans.values()) {
            register0(bean);
        }

        sortAll();
    }
}
