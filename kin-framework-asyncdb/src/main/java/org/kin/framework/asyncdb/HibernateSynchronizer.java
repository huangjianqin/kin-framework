package org.kin.framework.asyncdb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.List;

/**
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
