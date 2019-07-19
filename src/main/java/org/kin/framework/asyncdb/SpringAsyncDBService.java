package org.kin.framework.asyncdb;

import org.kin.framework.asyncdb.impl.DefaultAsyncDBStrategy;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;

/**
 * @author huangjianqin
 * @date 2019/7/19
 */
//不想引用该jar并使用spring时, 自动加载项目不使用的bean. 想用的话, 继承并使用@Component
//@Component
public class SpringAsyncDBService extends AsyncDBService implements ApplicationContextAware {
    private ApplicationContext springContext;

    @Override
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
                if (persistentClass != null) {
                    DBSynchronzier DBSynchronzier = springContext.getBean(persistentClass);
                    if (DBSynchronzier != null) {
                        class2Persistent.put(claxx, DBSynchronzier);
                        return DBSynchronzier;
                    } else {
                        throw new AsyncDBException("找不到类'" + claxx.getName() + "' 持久化类");
                    }
                } else {
                    throw new AsyncDBException("找不到类'" + claxx.getName() + "' 持久化类");
                }
            } else {
                throw new AsyncDBException("找不到类'" + claxx.getName() + "' 持久化类");
            }
        }

        return class2Persistent.get(claxx);
    }
}
