package org.kin.framework.asyncdb;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.kin.framework.spring.ConditionOnMissingBean;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void onApplicationEvent(@Nonnull ContextRefreshedEvent event) {
        //自动从Spring容器获取持久化实现类
        ApplicationContext context = event.getApplicationContext();

        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .enableClassInfo()
                .acceptPackages("")
                .scan()) {
            for (ClassInfo targetClassInfo : scanResult.getClassesWithAnnotation(DbSynchronzierClass.class.getCanonicalName())) {
                Class<?> targetClass = targetClassInfo.loadClass();
                DbSynchronzierClass anno = targetClass.getAnnotation(DbSynchronzierClass.class);
                Class<? extends DbSynchronzier> synchronzierClass = anno.type();
                DbSynchronzier dbSynchronzier = context.getBean(synchronzierClass);
                asyncDbService.register(targetClass, dbSynchronzier);
            }
        }

        for (AbstractEntityCache entityCache : context.getBeansOfType(AbstractEntityCache.class).values()) {
            //注册监听器
            asyncDbService.addListener(entityCache);
        }
    }
}
