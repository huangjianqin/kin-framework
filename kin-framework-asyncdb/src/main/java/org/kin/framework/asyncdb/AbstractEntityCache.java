package org.kin.framework.asyncdb;

import com.google.common.cache.Cache;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.HashUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 不能使用@Repository注解注入, 因为PersistenceExceptionTranslationPostProcessor会对有@Repository注解的实例进行增强,
 * 然而仅仅是增强接口方法, 类方法不会指向target instance相应的类方法实现, 相当于如果在外部调用类方法, 等同于调用代理类的类方法,
 * 原有的field字段存在可能值丢失(注解也有可能, 使用@Inherited)
 *
 * @author huangjianqin
 * @date 2020/12/27
 */
public abstract class AbstractEntityCache<PK extends Serializable, E extends AsyncDbEntity<PK>, S extends DbSynchronzier<PK, E>> implements EntityListener<E> {
    /** 缓存分段锁 */
    private final ReentrantLock[] locks;
    /** entity缓存 */
    private final Cache<PK, E> cache;
    /** 实体类 */
    protected final Class<E> entityClass;
    /** 正在delete的db entity */
    private final Map<PK, E> removing = new ConcurrentHashMap<>();
    /** 实体类对应的{@link DbSynchronzier} */
    @Autowired
    private S dbSynchronzier;

    @SuppressWarnings("unchecked")
    protected AbstractEntityCache(Cache<PK, E> cache) {
        locks = new ReentrantLock[8];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantLock();
        }
        this.cache = cache;

        List<Class<?>> genericTypes = ClassUtils.getSuperClassGenericRawTypes(getClass());
        entityClass = (Class<E>) genericTypes.get(1);
    }

    /**
     * 获取分段锁
     */
    private ReentrantLock getLock(PK pk) {
        return locks[HashUtils.efficientHash(pk, locks.length)];
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
            return entity;
        } finally {
            lock.unlock();
        }
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
    @Override
    public void onSuccess(E entity, DbOperation operation) {
        afterOperation(entity, operation, null);
    }

    @Override
    public void onError(E entity, DbOperation operation, Throwable ex) {
        afterOperation(entity, operation, ex);
        if (DbOperation.INSERT.equals(operation)) {
            //insert遇到异常, 重新提交db操作
            entity.insert();
        }
        if (DbOperation.UPDATE.equals(operation)) {
            //update遇到异常, 重新提交db操作
            entity.update();
        }
    }

    /**
     * db操作之后执行, 不管成功与否
     */
    private void afterOperation(AsyncDbEntity<?> entity, DbOperation operation, Throwable ex) {
        if (DbOperation.DELETE.equals(operation)) {
            afterDelete(entity);
        }
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
