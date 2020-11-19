package org.kin.framework.asyncdb;

import org.kin.framework.Closeable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2019/3/31
 * <p>
 * 一Entity(以hashcode分区)一条线程执行DB操作
 */
public class AsyncDBService implements Closeable {
    protected static final Logger log = LoggerFactory.getLogger(AsyncDBService.class);
    private static AsyncDBService INSTANCE;

    protected final Map<Class, DBSynchronzier> class2Persistent = new ConcurrentHashMap<>();
    private AsyncDBExecutor asyncDBExecutor;

    //---------------------------------------------------------------------------------------------------

    public static AsyncDBService getInstance() {
        if (INSTANCE == null) {
            synchronized (AsyncDBService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AsyncDBService();
                }
            }
        }
        return INSTANCE;
    }

    //---------------------------------------------------------------------------------------------------

    public AsyncDBService() {
        monitorJVMClose();
    }

    public void init(int num, AsyncDBStrategy asyncDBStrategy) {
        asyncDBExecutor = new AsyncDBExecutor();
        asyncDBExecutor.init(num, asyncDBStrategy);
    }

    /**
     * 手动注册持久化实现类
     */
    public void register(Class<?> claxx, DBSynchronzier DBSynchronzier) {
        Type interfaceType = null;
        for (Type type : DBSynchronzier.getClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(DBSynchronzier.class)) {
                interfaceType = type;
                break;
            }
            if (type instanceof Class && type.equals(DBSynchronzier.class)) {
                interfaceType = type;
                break;
            }
        }

        if (interfaceType != null) {
            Type entityType = ((ParameterizedType) interfaceType).getActualTypeArguments()[0];
            Class<?> entityClass;
            if (entityType instanceof ParameterizedType) {
                entityClass = (Class<?>) ((ParameterizedType) entityType).getRawType();
            } else {
                entityClass = (Class<?>) entityType;
            }

            if (entityClass.isAssignableFrom(claxx)) {
                //校验通过
                class2Persistent.put(claxx, DBSynchronzier);
            }
        }
    }

    protected DBSynchronzier getAsyncPersistent(AsyncDBEntity asyncDBEntity) {
        return class2Persistent.get(asyncDBEntity.getClass());
    }

    boolean dbOpr(AsyncDBEntity asyncDBEntity, DBOperation operation) {
        asyncDBEntity.serialize();
        try {
            DBSynchronzier DBSynchronzier = getAsyncPersistent(asyncDBEntity);

            if (DBSynchronzier != null) {
                if (asyncDBEntity.getAsyncPersistent() == null) {
                    asyncDBEntity.setAsyncPersistent(DBSynchronzier);
                }
                if (asyncDBEntity.isCanPersist(operation)) {
                    if (DBOperation.Update.equals(operation) && !asyncDBEntity.tryUpdate()) {
                        //队列中有该实体的update操作, 不需要再执行了, 直接返回true, 表示操作成功的
                        return true;
                    }

                    return asyncDBExecutor.submit(asyncDBEntity);
                }
            }
        } catch (Exception e) {
            if (DBOperation.Update.equals(operation)) {
                asyncDBEntity.resetUpdating();
            }
            log.error(e.getMessage(), e);
        }

        return false;
    }

    @Override
    public void close() {
        class2Persistent.clear();
        asyncDBExecutor.close();
    }
}
