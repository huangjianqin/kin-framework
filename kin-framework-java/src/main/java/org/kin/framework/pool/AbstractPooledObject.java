package org.kin.framework.pool;

/**
 * 池化对象抽象, 实现更加便捷友好的api, 否则需要user每次定义池化对象都要缓存{@link ObjectPool.Handle},
 * 并手动调用{@link ObjectPool.Handle#recycle()}或者{@link Recycler#recycle(Object, ObjectPool.Handle)}接口
 *
 * @author huangjianqin
 * @date 2021/11/6
 */
public abstract class AbstractPooledObject implements Recyclable {
    protected final transient ObjectPool.Handle handle;

    protected AbstractPooledObject(ObjectPool.Handle handle) {
        this.handle = handle;
    }

    @Override
    public final boolean recycle() {
        beforeRecycle();
        handle.recycle();
        return true;
    }

    /**
     * 用于实现类执行clear逻辑, 方便复用
     */
    protected abstract void beforeRecycle();
}
