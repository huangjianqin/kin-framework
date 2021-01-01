package org.kin.framework.asyncdb;

import com.google.common.cache.CacheBuilder;

import java.io.Serializable;

/**
 * 强引用cache
 *
 * @author huangjianqin
 * @date 2021/1/2
 */
public abstract class StrongValueEntityCache<PK extends Serializable, E extends AsyncDbEntity<PK>, S extends DbSynchronzier<PK, E>>
        extends AbstractEntityCache<PK, E, S> {
    protected StrongValueEntityCache() {
        super(CacheBuilder.newBuilder().build());
    }
}

