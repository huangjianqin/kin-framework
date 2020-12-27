package org.kin.framework.asyncdb;

/**
 * entity操作后触发的listener
 *
 * @author huangjianqin
 * @date 2020/12/26
 */
public interface EntityListener<E extends AsyncDbEntity<?>> {
    /**
     * db操作成功
     *
     * @param entity    实体
     * @param operation db操作
     */
    void onSuccess(E entity, DbOperation operation);

    /**
     * db操作失败
     *
     * @param entity    实体
     * @param operation db操作
     * @param ex        异常
     */
    void onError(E entity, DbOperation operation, Throwable ex);
}
