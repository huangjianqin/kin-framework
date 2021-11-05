package org.kin.framework.pool;

import com.google.common.base.Preconditions;

/**
 * Light-weight object pool.
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @param <T> the type of the pooled object
 * @author huangjianqin
 * @date 2021/11/4
 */
public abstract class ObjectPool<T> {

    protected ObjectPool() {
    }

    /**
     * 从对象池里获取对象, 如果没有可复用实例, 则通过{@link ObjectCreator#newObject(Handle)}构建新的
     * <p>
     * Get a {@link Object} from the {@link ObjectPool}. The returned {@link Object} may be created via
     * {@link ObjectCreator#newObject(Handle)} if no pooled {@link Object} is ready to be reused.
     */
    public abstract T get();

    /**
     * 用于池化对象一旦需要复用时通知{@link ObjectPool}逻辑
     * <p>
     * Handle for an pooled {@link Object} that will be used to notify the {@link ObjectPool} once it can
     * reuse the pooled {@link Object} again.
     */
    public interface Handle {
        /**
         * 复用对象
         * recycle this instance if possible and so make it ready to be reused.
         */
        void recycle();
    }

    /**
     * 绑定一个{@link Handle}并在想复用时调用{@link Handle#recycle()}
     * <p>
     * Creates a new Object which references the given {@link Handle} and calls {@link Handle#recycle()} once
     * it can be re-used.
     *
     * @param <T> the type of the pooled object
     */
    public interface ObjectCreator<T> {
        /**
         * 创建对象
         * <p>
         * Creates an returns a new {@link Object} that can be used and later recycled via
         * {@link Handle#recycle()}.
         */
        T newObject(Handle handle);
    }

    /**
     * 构建新的对象池
     * <p>
     * Creates a new {@link ObjectPool} which will use the given {@link ObjectCreator} to create the {@link Object}
     * that should be pooled.
     */
    public static <T> ObjectPool<T> newPool(ObjectCreator<T> creator) {
        Preconditions.checkNotNull(creator);
        return new RecyclerObjectPool<>(creator);
    }

    /**
     * 构建新的对象池
     */
    public static <T> ObjectPool<T> newPool(int maxCapacityPerThread, ObjectCreator<T> creator) {
        Preconditions.checkNotNull(creator);
        Preconditions.checkArgument(maxCapacityPerThread > 0);
        return new RecyclerObjectPool<>(maxCapacityPerThread, creator);
    }

    public static final ObjectPool.Handle NOOP_HANDLE = () -> {
        //do nothing
    };
    //------------------------------------------------------------------------

    /**
     * 简单而通用的线程池实现类
     */
    private static final class RecyclerObjectPool<T> extends ObjectPool<T> {
        private final Recycler<T> recycler;

        RecyclerObjectPool(ObjectCreator<T> creator) {
            recycler = new Recycler<T>() {
                @Override
                protected T newObject(Handle handle) {
                    return creator.newObject(handle);
                }
            };
        }

        RecyclerObjectPool(int maxCapacityPerThread, ObjectCreator<T> creator) {
            recycler = new Recycler<T>(maxCapacityPerThread) {
                @Override
                protected T newObject(Handle handle) {
                    return creator.newObject(handle);
                }
            };
        }

        @Override
        public T get() {
            return recycler.get();
        }
    }
}
