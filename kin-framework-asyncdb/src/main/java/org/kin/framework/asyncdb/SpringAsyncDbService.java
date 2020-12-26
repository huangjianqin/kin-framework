package org.kin.framework.asyncdb;

import org.kin.framework.utils.SysUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

/**
 * @author huangjianqin
 * @date 2019/7/19
 */
public class SpringAsyncDbService extends AsyncDbService implements ApplicationContextAware {
    private ApplicationContext springContext;

    @Override
    @NonNull
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        springContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        super.init(SysUtils.CPU_NUM, new DefaultAsyncDbStrategy());
    }

    /**
     * 自动从Spring容器获取持久化实现类
     */
    @Override
    protected AbstractDbSynchronzier<?> getAsyncPersistent(AsyncDbEntity asyncDbEntity) {
        Class<?> claxx = asyncDbEntity.getClass();
        if (!class2Persistent.containsKey(claxx)) {
            DbSynchronzierClass persistentAnnotation = claxx.getAnnotation(DbSynchronzierClass.class);
            if (persistentAnnotation != null) {
                Class<? extends AbstractDbSynchronzier<?>> persistentClass = persistentAnnotation.type();
                AbstractDbSynchronzier<?> dbSynchronzier = springContext.getBean(persistentClass);
                class2Persistent.put(claxx, dbSynchronzier);
                return dbSynchronzier;
            } else {
                throw new AsyncDbException("找不到类'" + claxx.getName() + "' 持久化类");
            }
        }

        return class2Persistent.get(claxx);
    }
}
