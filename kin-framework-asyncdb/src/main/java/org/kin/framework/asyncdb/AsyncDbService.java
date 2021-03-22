package org.kin.framework.asyncdb;

import org.kin.framework.Closeable;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.SysUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一Entity(以hashcode分区)一条线程执行DB操作
 *
 * @author huangjianqin
 * @date 2019/3/31
 */
@SuppressWarnings("rawtypes")
public final class AsyncDbService implements Closeable, LoggerOprs {
    /** 单例 */
    private static AsyncDbService INSTANCE;

    /** key -> {@link AsyncDbEntity} class, value -> 对应的{@link DbSynchronzier}实现 */
    protected final Map<Class<?>, DbSynchronzier<?, ? extends AsyncDbEntity<?>>> class2Synchronzier = new ConcurrentHashMap<>();
    /** db worker */
    private final AsyncDbExecutor workers = new AsyncDbExecutor();

    //---------------------------------------------------------------------------------------------------
    public static AsyncDbService getInstance() {
        if (INSTANCE == null) {
            synchronized (AsyncDbService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AsyncDbService();
                    INSTANCE.init(SysUtils.CPU_NUM, new AsyncDbStrategy() {
                        @Override
                        public int getOprNum() {
                            return 10;
                        }

                        @Override
                        public int getRetryTimes() {
                            return 2;
                        }

                        @Override
                        public int getDuration(int size) {
                            //自适应
                            if (size > 50) {
                                return 200;
                            }
                            return 1000;
                        }
                    });
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
        workers.init(threadNum, asyncDbStrategy);
    }

    /**
     * 手动注册持久化实现类
     */
    @SuppressWarnings("unchecked")
    public void register(Class<?> claxx, DbSynchronzier dbSynchronzier) {
        //校验dbSynchronzier的泛型类是否于claxx一致
        Class<? extends DbSynchronzier> implClass = dbSynchronzier.getClass();
        List<Class<?>> genericTypes = ClassUtils.getSuperInterfacesGenericRawTypes(DbSynchronzier.class, implClass);
        if (CollectionUtils.isEmpty(genericTypes)) {
            throw new IllegalArgumentException(String.format("%s doesn't have generic param", implClass));
        }

        if (!genericTypes.get(1).isAssignableFrom(claxx)) {
            throw new IllegalArgumentException(String.format("%s's generic param is not assignable from %s'", implClass, claxx));
        }

        //校验通过过
        class2Synchronzier.put(claxx, dbSynchronzier);
    }

    /**
     * 获取db entity对应的{@link DbSynchronzier}
     */
    protected DbSynchronzier getDbSynchronzier(Class<? extends AsyncDbEntity> entityClass) {
        return class2Synchronzier.get(entityClass);
    }

    /**
     * 执行db操作
     */
    @SuppressWarnings("unchecked")
    boolean dbOpr(AsyncDbEntity asyncDbEntity, DbOperation operation) {
        asyncDbEntity.serialize();
        DbSynchronzier dbSynchronzier = getDbSynchronzier(asyncDbEntity.getClass());
        try {
            if (dbSynchronzier != null) {
                if (asyncDbEntity.getDbSynchronzier() == null) {
                    asyncDbEntity.setDbSynchronzier(dbSynchronzier);
                }
                if (asyncDbEntity.isCanPersist(operation)) {
                    return workers.submit(asyncDbEntity);
                }
            } else {
                throw new IllegalStateException(
                        String.format("Entity '%s' does not have DbSynchronzier", asyncDbEntity.getClass().getName()));
            }
        } catch (Exception e) {
            error("", e);
        }

        return false;
    }

    /**
     * 添加{@link EntityListener}
     */
    public void addListener(EntityListener<?> listener) {
        this.workers.addListener(listener);
    }

    /**
     * 批量添加{@link EntityListener}
     */
    public void addListeners(EntityListener<?>... listeners) {
        addListeners(Arrays.asList(listeners));
    }

    /**
     * 批量添加{@link EntityListener}
     */
    public void addListeners(Collection<EntityListener<?>> listeners) {
        this.workers.addListeners(listeners);
    }

    @Override
    public void close() {
        class2Synchronzier.clear();
        workers.close();
    }
}
