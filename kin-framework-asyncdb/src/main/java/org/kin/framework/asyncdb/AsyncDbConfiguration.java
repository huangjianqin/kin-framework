package org.kin.framework.asyncdb;

import org.kin.framework.spring.ConditionOnMissingBean;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;

import javax.annotation.Nonnull;

/**
 * spring容器refresh后, 自动注册db entity与DbSynchronzier的一对一关系
 *
 * @author huangjianqin
 * @date 2019/7/19
 */
@Order
public class AsyncDbConfiguration implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired
    private AsyncDbService asyncDbService;

    @Bean
    @ConditionOnMissingBean(AsyncDbService.class)
    public AsyncDbService asyncDbService() {
        //默认使用AsyncDbService单例
        return AsyncDbService.getInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        //自动从Spring容器获取持久化实现类
        ApplicationContext context = event.getApplicationContext();

        Reflections reflections = new Reflections("", new TypeAnnotationsScanner(), new SubTypesScanner());
        for (Class<?> targetClass : reflections.getTypesAnnotatedWith(DbSynchronzierClass.class)) {
            DbSynchronzierClass anno = targetClass.getAnnotation(DbSynchronzierClass.class);
            Class<? extends DbSynchronzier> synchronzierClass = anno.type();
            DbSynchronzier dbSynchronzier = context.getBean(synchronzierClass);
            asyncDbService.register(targetClass, dbSynchronzier);
        }

        for (AbstractEntityCache entityCache : context.getBeansOfType(AbstractEntityCache.class).values()) {
            //注册监听器
            asyncDbService.addListener(entityCache);
        }
    }
}
