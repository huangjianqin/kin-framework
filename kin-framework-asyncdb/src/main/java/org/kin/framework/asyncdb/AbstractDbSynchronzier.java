package org.kin.framework.asyncdb;

import org.kin.framework.utils.ExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 定义DB基本操作
 *
 * @author huangjianqin
 * @date 2019/3/31
 */
public abstract class AbstractDbSynchronzier<E extends AsyncDbEntity> {
    /** entity listeners */
    private final CopyOnWriteArrayList<EntityListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 定义Insert操作
     */
    protected abstract void doInsert(E entity);

    /**
     * 定义Update操作
     */
    protected abstract void doUpdate(E entity);

    /**
     * 定义Delete操作
     */
    protected abstract void doDelete(E entity);

    /**
     * insert, 内部使用
     */
    void insert(E entity) {
        doDbOperation(entity, this::doInsert, DbOperation.Insert);
    }

    /**
     * update, 内部使用
     */
    void update(E entity) {
        doDbOperation(entity, this::doUpdate, DbOperation.Update);
    }

    /**
     * delete, 内部使用
     */
    void delete(E entity) {
        doDbOperation(entity, this::delete, DbOperation.Delete);
    }

    /**
     * 执行db操作
     */
    private void doDbOperation(E entity, Consumer<E> exec, DbOperation operation) {
        try {
            exec.accept(entity);
        } catch (Exception e) {
            for (EntityListener listener : listeners) {
                try {
                    listener.onError(entity, operation, e);
                } catch (Exception ex) {
                    ExceptionUtils.throwExt(ex);
                }
            }
            return;
        }

        for (EntityListener listener : listeners) {
            try {
                listener.onSuccess(entity, operation);
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }
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
