package org.kin.framework.asyncdb;

/**
 * 定义DB基本操作
 *
 * @author huangjianqin
 * @date 2019/3/31
 */
public interface DbSynchronzier<E extends AsyncDbEntity> {
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
}
