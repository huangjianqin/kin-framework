package org.kin.framework.asyncdb;

import com.google.common.cache.Cache;
import org.kin.framework.utils.ClassUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author huangjianqin
 * @date 2020/12/27
 */
public abstract class AbstractEntityCache<PK, E extends AsyncDbEntity<PK>> implements EntityListener<E> {
    /** 缓存分段锁 */
    private final ReentrantLock[] locks;
    /** entity缓存 */
    private final Cache<PK, E> cache;
    /** 实体类 */
    private final Class<E> entityClass;
    /** 正在delete的db entity */
    private final Map<PK, E> removing = new ConcurrentHashMap<>();
    /** 实体类对应的{@link DbSynchronzier} */
    private DbSynchronzier<PK, E> dbSynchronzier;

    @SuppressWarnings("unchecked")
    protected AbstractEntityCache(Cache<PK, E> cache) {
        locks = new ReentrantLock[8];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
        this.cache = cache;

        List<Class<?>> genericTypes = ClassUtils.getSuperClassGenericActualTypes(getClass());
        entityClass = (Class<E>) genericTypes.get(1);
    }

    /**
     * 获取分段锁
     */
    private ReentrantLock getLock(PK pk) {
        return locks[pk.hashCode() % locks.length];
    }

    /**
     * 获取db entity
     *
     * @param pk 主键
     * @return db entity
     */
    @SuppressWarnings("unchecked")
    public E load(PK pk, Object... args) {
        E cachedEntity = loadFromCache(pk);
        if (Objects.nonNull(cachedEntity)) {
            return cachedEntity;
        }

        //此处有锁
        ReentrantLock lock = getLock(pk);
        lock.lock();
        try {
            E entity = loadFromDb(pk);
            if (Objects.isNull(entity) || removing.containsKey(pk)) {
                //删除操作还没入库, 但在队列中, 仍然new db entity
                entity = initEntity(pk, args);
                entity.insert();

                removing.remove(pk);
            }

            cache.put(pk, entity);
        } finally {
            lock.unlock();
        }

        //理论上不会走到这里
        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 从数据库加载db entity
     *
     * @param pk 主键
     * @return db entity
     */
    private E loadFromDb(PK pk) {
        return dbSynchronzier.get(pk);
    }

    /**
     * 从缓存加载db entity
     *
     * @param pk 主键
     * @return db entity, 缓存没有则null
     */
    private E loadFromCache(PK pk) {
        return cache.getIfPresent(pk);
    }

    /**
     * 初始化db entity
     *
     * @param pk 主键
     * @return db entity
     */
    protected abstract E initEntity(PK pk, Object... args);

    /**
     * 想对缓存中的实体执行delete操作应该使用该方法, 而不是单独调用{@link AsyncDbEntity}.delete()
     * 移除缓存并执行delete
     */
    public void invalidAndDelete(PK pk) {
        if (removing.containsKey(pk)) {
            return;
        }

        ReentrantLock lock = getLock(pk);
        lock.lock();
        try {
            if (removing.containsKey(pk)) {
                return;
            }

            E remove = cache.getIfPresent(pk);
            if (Objects.nonNull(remove)) {
                removing.put(pk, remove);
                remove.delete();
                cache.invalidate(pk);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 仅仅将db entity从缓存中移除
     */
    public void invalid(PK pk) {
        ReentrantLock lock = getLock(pk);
        lock.lock();
        try {
            cache.invalidate(pk);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 缓存清空
     */
    public void invalidAll(PK pk) {
        cache.invalidateAll();
    }

    /**
     * 对于批量加载的, 保证只使用缓存的, 也就是缓存有就取缓存, 没有就放入缓存
     */
    public List<E> transform(List<E> entities) {
        List<E> result = new ArrayList<>(entities.size());
        for (E entity : entities) {
            PK pk = entity.getPrimaryKey();
            ReentrantLock lock = getLock(pk);
            lock.lock();
            try {
                ConcurrentMap<PK, E> cacheMap = cache.asMap();
                result.add(cacheMap.putIfAbsent(pk, entity));
            } finally {
                lock.unlock();
            }
        }

        return result;
    }

    /**
     * 加载表中所有entity到缓存中
     */
    public List<E> loadAll() {
        List<E> allEntities = dbSynchronzier.getAll();
        return transform(allEntities);
    }

    //----------------------------------------------------------------------------------------------------------------

    /**
     * 用于设置db entity对应的{@link DbSynchronzier}
     */
    synchronized void updateDbSynchronzier(DbSynchronzier<PK, E> dbSynchronzier) {
        if (Objects.nonNull(this.dbSynchronzier)) {
            throw new IllegalStateException("entity cache's dbSynchronzier can only update once");
        }
        this.dbSynchronzier = dbSynchronzier;
    }

    @Override
    public void onSuccess(E entity, DbOperation operation) {
        afterOperation(entity, operation, null);
    }

    @Override
    public void onError(E entity, DbOperation operation, Throwable ex) {
        afterOperation(entity, operation, ex);
        if (DbOperation.Insert.equals(operation)) {
            //insert遇到异常, 重新提交db操作
            entity.insert();
        }
        if (DbOperation.Update.equals(operation)) {
            //update遇到异常, 重新提交db操作
            entity.update();
        }
    }

    /**
     * db操作之后执行, 不管成功与否
     */
    private void afterOperation(AsyncDbEntity<?> entity, DbOperation operation, Throwable ex) {
        if (DbOperation.Update.equals(operation)) {
            afterUpdate(entity);
        }

        if (DbOperation.Delete.equals(operation)) {
            afterDelete(entity);
        }
    }

    /**
     * update操作之后执行, 不管成功与否
     */
    private void afterUpdate(AsyncDbEntity<?> entity) {
        entity.resetUpdating();
    }

    /**
     * delete操作之后执行, 不管成功与否
     */
    private void afterDelete(AsyncDbEntity<?> entity) {
        removing.remove(entity.getPrimaryKey());
    }

    //getter
    public Class<E> getEntityClass() {
        return entityClass;
    }
}
