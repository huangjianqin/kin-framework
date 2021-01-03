package org.kin.framework.asyncdb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

/**
 * 不能使用@Repository注解注入, 因为PersistenceExceptionTranslationPostProcessor会对有@Repository注解的实例进行增强,
 * 然而仅仅是增强接口方法, 类方法不会指向target instance相应的类方法实现, 相当于如果在外部调用类方法, 等同于调用代理类的类方法,
 * 原有的field字段存在可能值丢失(注解也有可能, 使用@Inherited)
 *
 * @author huangjianqin
 * @date 2021/1/2
 */
public abstract class HibernateSynchronizer<PK extends Serializable, E extends AsyncDbEntity<PK>, JPA extends JpaRepository<E, PK>>
        implements DbSynchronzier<PK, E> {
    @Autowired
    protected JPA repository;

    @Override
    public final void insert(E entity) {
        repository.saveAndFlush(entity);
    }

    @Override
    public final void update(E entity) {
        repository.saveAndFlush(entity);
    }

    @Override
    public final void delete(E entity) {
        repository.delete(entity);
    }

    @Override
    public final E get(PK pk) {
        return repository.findById(pk).orElse(null);
    }

    @Override
    public final List<E> getAll() {
        return repository.findAll();
    }
}
