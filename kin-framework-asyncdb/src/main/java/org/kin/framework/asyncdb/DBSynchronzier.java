package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/3/31
 * <p>
 * 定义DB基本操作
 */
public interface DBSynchronzier<E extends AsyncDBEntity> {
    void insert(E entity);

    void update(E entity);

    void delete(E entity);
}
