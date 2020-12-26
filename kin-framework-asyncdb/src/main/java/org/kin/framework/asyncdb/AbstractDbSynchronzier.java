package org.kin.framework.asyncdb;

import org.kin.framework.log.LoggerOprs;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 定义DB基本操作
 *
 * @author huangjianqin
 * @date 2019/3/31
 */
public abstract class AbstractDbSynchronzier<E extends AsyncDbEntity> implements LoggerOprs {
    /** entity listeners */
    private final CopyOnWriteArrayList<EntityListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 定义Insert操作
     */
    protected abstract boolean doInsert(E entity);

    /**
     * 定义Update操作
     */
    protected abstract boolean doUpdate(E entity);

    /**
     * 定义Delete操作
     */
    protected abstract boolean doDelete(E entity);

    /**
     * insert, 内部使用
     */
    boolean insert(E entity) {
        return doDbOperation(entity, () -> doInsert(entity), DbOperation.Insert);
    }

    /**
     * update, 内部使用
     */
    boolean update(E entity) {
        return doDbOperation(entity, () -> doUpdate(entity), DbOperation.Update);
    }

    /**
     * delete, 内部使用
     */
    boolean delete(E entity) {
        return doDbOperation(entity, () -> doDelete(entity), DbOperation.Delete);
    }

    /**
     * 执行db操作
     */
    private boolean doDbOperation(E entity, Callable<Boolean> exec, DbOperation operation) {
        boolean result = false;
        try {
            result = exec.call();
        } catch (Exception e) {
            error("", e);
            for (EntityListener listener : listeners) {
                try {
                    listener.onError(entity, operation, e);
                } catch (Exception ex) {
                    error("", ex);
                }
            }
            return result;
        }

        for (EntityListener listener : listeners) {
            try {
                listener.onSuccess(entity, operation);
            } catch (Exception e) {
                error("", e);
            }
        }

        return result;
    }

    /**
     * 添加{@link EntityListener}
     */
    public void addListener(EntityListener listener) {
        this.listeners.add(listener);
    }

    /**
     * 批量添加{@link EntityListener}
     */
    public void addListeners(EntityListener... listeners) {
        addListeners(Arrays.asList(listeners));
    }

    /**
     * 批量添加{@link EntityListener}
     */
    public void addListeners(Collection<EntityListener> listeners) {
        this.listeners.addAll(listeners);
    }
}
