package org.kin.framework.asyncdb;

import java.util.List;

/**
 * 定义DB基本操作
 *
 * @author huangjianqin
 * @date 2019/3/31
 */
public interface DbSynchronzier<PK, E extends AsyncDbEntity<PK>> {
    /**
     * 定义Insert操作
     */
    boolean insert(E entity);

    /**
     * 定义Update操作
     */
    boolean update(E entity);

    /**
     * 定义Delete操作
     */
    boolean delete(E entity);

    /**
     * 根据主键获取db entity
     *
     * @param pk 主键
     * @return db entity
     */
    E get(PK pk);

    /**
     * 返回表中所有entity
     */
    List<E> getAll();
}
