package org.kin.framework.concurrent;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/3/10
 */
interface EventExecutorGroup extends ScheduledExecutorService {
    /**
     * @return 选择一个EventExecutor返回
     */
    EventExecutor next();

    @Override
    default ScheduledFuture<?> schedule(@Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
        return next().schedule(command, delay, unit);
    }

    @Override
    default <V> ScheduledFuture<V> schedule(@Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
        return next().schedule(callable, delay, unit);
    }

    @Override
    default ScheduledFuture<?> scheduleAtFixedRate(@Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
        return next().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    default ScheduledFuture<?> scheduleWithFixedDelay(@Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
        return next().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    default List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override
    default <T> Future<T> submit(@Nonnull Callable<T> task) {
        return next().submit(task);
    }

    @Override
    default <T> Future<T> submit(@Nonnull Runnable task, T result) {
        return next().submit(task, result);
    }

    @Override
    default Future<?> submit(@Nonnull Runnable task) {
        return next().submit(task);
    }

    @Override
    default <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return next().invokeAll(tasks);
    }

    @Override
    default <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return next().invokeAll(tasks, timeout, unit);
    }

    @Override
    default <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return next().invokeAny(tasks);
    }

    @Override
    default <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return next().invokeAny(tasks, timeout, unit);
    }

    @Override
    default void execute(@Nonnull Runnable command) {
        next().execute(command);
    }
}
