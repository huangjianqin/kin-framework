package org.kin.framework.event.impl;

import org.kin.framework.event.annotation.Listener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 获取注释有@Listener的bean并注册
 *
 * @author huangjianqin
 * @date 2019/7/19
 */
@Component
public class SpringListenerManager implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(Listener.class);
        for (Object bean : beans.values()) {
            SimpleListenerManager.instance().register0(bean);
        }

        SimpleListenerManager.instance().sortAll();
    }
}
