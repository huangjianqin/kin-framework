package org.kin.framework.asyncdb;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;

/**
 * spring容器refresh后, 自动注册db entity与DbSynchronzier的一对一关系
 *
 * @author huangjianqin
 * @date 2019/7/19
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AsyncDbConfiguration implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        //自动从Spring容器获取持久化实现类
        ApplicationContext context = event.getApplicationContext();

        AsyncDbService asyncDbService;
        try {
            //优先使用自定义的AsyncDbService
            asyncDbService = context.getBean(AsyncDbService.class);
        } catch (Exception e) {
            //没有找到自定义, 则默认
            asyncDbService = AsyncDbService.getInstance();
        }

        Reflections reflections = new Reflections(new TypeAnnotationsScanner());
        for (Class<?> targetClass : reflections.getTypesAnnotatedWith(DbSynchronzierClass.class)) {
            DbSynchronzierClass anno = targetClass.getAnnotation(DbSynchronzierClass.class);
            Class<? extends DbSynchronzier<?>> persistentClass = anno.type();
            DbSynchronzier<?> dbSynchronzier = context.getBean(persistentClass);
            asyncDbService.register(targetClass, dbSynchronzier);
        }
    }
}
