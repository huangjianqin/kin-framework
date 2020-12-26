package org.kin.framework.asyncdb;

import org.kin.framework.Closeable;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.SysUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一Entity(以hashcode分区)一条线程执行DB操作
 *
 * @author huangjianqin
 * @date 2019/3/31
 */
public class AsyncDbService implements Closeable, LoggerOprs {
    /** 单例 */
    private static AsyncDbService INSTANCE;
    /** key -> {@link AsyncDbEntity} class, value -> 对应的{@link AbstractDbSynchronzier}实现 */
    protected final Map<Class<?>, AbstractDbSynchronzier<?>> class2Persistent = new ConcurrentHashMap<>();
    /** db worker */
    private AsyncDbExecutor asyncDbExecutor;

    //---------------------------------------------------------------------------------------------------

    public static AsyncDbService getInstance() {
        if (INSTANCE == null) {
            synchronized (AsyncDbService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AsyncDbService();
                    INSTANCE.init(SysUtils.CPU_NUM, new DefaultAsyncDbStrategy());
                }
            }
        }
        return INSTANCE;
    }

    //---------------------------------------------------------------------------------------------------

    public AsyncDbService() {
        monitorJVMClose();
    }

    public void init(int threadNum, AsyncDbStrategy asyncDbStrategy) {
        asyncDbExecutor = new AsyncDbExecutor();
        asyncDbExecutor.init(threadNum, asyncDbStrategy);
    }

    /**
     * 手动注册持久化实现类
     */
    public void register(Class<?> claxx, AbstractDbSynchronzier<?> dbSynchronzier) {
        Type interfaceType = null;
        for (Type type : dbSynchronzier.getClass().getGenericInterfaces()) {
            if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType().equals(AbstractDbSynchronzier.class)) {
                interfaceType = type;
                break;
            }
            if (type instanceof Class && type.equals(AbstractDbSynchronzier.class)) {
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
                class2Persistent.put(claxx, dbSynchronzier);
            }
        }
    }

    protected AbstractDbSynchronzier<?> getAsyncPersistent(AsyncDbEntity asyncDbEntity) {
        return class2Persistent.get(asyncDbEntity.getClass());
    }

    /**
     * 执行db操作
     */
    boolean dbOpr(AsyncDbEntity asyncDbEntity, DbOperation operation) {
        asyncDbEntity.serialize();
        AbstractDbSynchronzier<?> dbSynchronzier = getAsyncPersistent(asyncDbEntity);
        try {
            if (dbSynchronzier != null) {
                if (asyncDbEntity.getDbSynchronzier() == null) {
                    asyncDbEntity.setDbSynchronzier(dbSynchronzier);
                }
                if (asyncDbEntity.isCanPersist(operation)) {
                    if (DbOperation.Update.equals(operation) && !asyncDbEntity.tryUpdate()) {
                        //队列中有该实体的update操作, 不需要再执行了, 直接返回true, 表示操作成功的
                        return true;
                    }

                    return asyncDbExecutor.submit(asyncDbEntity);
                }
            }
        } catch (Exception e) {
            if (DbOperation.Update.equals(operation)) {
                asyncDbEntity.resetUpdating();
            }
            error("", e);
        }

        return false;
    }

    @Override
    public void close() {
        class2Persistent.clear();
        asyncDbExecutor.close();
    }
}
