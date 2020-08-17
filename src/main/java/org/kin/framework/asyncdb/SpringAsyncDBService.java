package org.kin.framework.asyncdb;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import javax.annotation.PostConstruct;

/**
 * @author huangjianqin
 * @date 2019/7/19
 */
public class SpringAsyncDBService extends AsyncDBService implements ApplicationContextAware {
    private ApplicationContext springContext;

    @Override
    @NonNull
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        springContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        super.init(10, new DefaultAsyncDBStrategy());
    }

    /**
     * 自动从Spring容器获取持久化实现类
     */
    @Override
    protected DBSynchronzier getAsyncPersistent(AsyncDBEntity asyncDBEntity) {
        Class claxx = asyncDBEntity.getClass();
        if (!class2Persistent.containsKey(claxx)) {
            PersistentClass persistentAnnotation = (PersistentClass) claxx.getAnnotation(PersistentClass.class);
            if (persistentAnnotation != null) {
                Class<? extends DBSynchronzier> persistentClass = persistentAnnotation.type();
                DBSynchronzier dbSynchronzier = springContext.getBean(persistentClass);
                class2Persistent.put(claxx, dbSynchronzier);
                return dbSynchronzier;
            } else {
                throw new AsyncDBException("找不到类'" + claxx.getName() + "' 持久化类");
            }
        }

        return class2Persistent.get(claxx);
    }
}
