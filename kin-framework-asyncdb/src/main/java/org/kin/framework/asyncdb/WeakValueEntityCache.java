package org.kin.framework.asyncdb;

import com.google.common.cache.CacheBuilder;

import java.io.Serializable;

/**
 * 弱引用cache
 *
 * @author huangjianqin
 * @date 2021/1/2
 */
public abstract class WeakValueEntityCache<PK extends Serializable, E extends AsyncDbEntity<PK>, S extends DbSynchronzier<PK, E>>
        extends AbstractEntityCache<PK, E, S> {
    protected WeakValueEntityCache() {
        super(CacheBuilder.newBuilder().weakValues().build());
    }
}