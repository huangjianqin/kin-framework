/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
public class DefaultPromise<V> implements Promise<V> {
    private static final Logger log = LoggerFactory.getLogger(DefaultPromise.class);
    private static final int MAX_LISTENER_STACK_DEPTH = Math.min(8,
            SysUtils.getIntSysProperty("kin.defaultPromise.maxListenerStackDepth", 8));

    /** 原子更新{@link #result} */
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DefaultPromise, Object> RESULT_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DefaultPromise.class, Object.class, "result");
    /** 当{@link #success(Object)}参数为null时, 用来标识该Promise成功的唯一对象 */
    private static final Object SUCCESS = new Object();
    /** 只要{@link #result}为null, {@link #cancel(boolean)}就可以执行成功, 将{@link #result}set为这个对象就是用来阻止{@link #cancel(boolean)}执行成功 */
    private static final Object UNCANCELLABLE = new Object();
    /** Promise被取消时, 给{@link #result}设置的对象 */
    private static final CauseHolder CANCELLATION_CAUSE_HOLDER = new CauseHolder(
            StacklessCancellationException.newInstance(DefaultPromise.class, "cancel(...)"));
    /** Promise被取消异常栈信息 */
    private static final StackTraceElement[] CANCELLATION_STACK = CANCELLATION_CAUSE_HOLDER.cause.getStackTrace();

    /** Promise操作结果 */
    private volatile Object result;
    /** 执行触发listener逻辑的{@link Executor} */
    private final Executor executor;
    /**
     * One or more listeners. Can be a {@link PromiseListener} or a {@link DefaultPromiseListeners}.
     * If {@code null}, it means either 1) no listeners were added yet or 2) all listeners were notified.
     * <p>
     * Threading - synchronized(this). We must support adding listeners when there is no EventExecutor.
     * <p>
     * 一个或多个listeners. 可以是{@link PromiseListener}或者{@link DefaultPromiseListeners}
     * 对当前Promise加锁. 必须支持当没有{@link EventExecutor}时也可以add listeners
     */
    protected Object listeners;
    /**
     * Threading - synchronized(this). We are required to hold the monitor to use Java's underlying wait()/notifyAll().
     * <p>
     * 对当前Promise加锁. 用java底层的wait()/notifyAll()来阻塞和唤醒线程
     */
    private short waiters;

    /**
     * Threading - synchronized(this). We must prevent concurrent notification and FIFO listener notification if the
     * executor changes.
     * <p>
     * 对当前Promise加锁. 防止多线程操作触发listeners
     */
    private boolean notifyingListeners;

    /**
     * Creates a new instance.
     * <p>
     * It is preferable to use {@link EventExecutor#newPromise()} to create a new promise
     *
     * @param executor the {@link EventExecutor} which is used to notify the promise once it is complete.
     *                 It is assumed this executor will protect against {@link StackOverflowError} exceptions.
     *                 The executor may be used to avoid {@link StackOverflowError} by executing a {@link Runnable} if the stack
     *                 depth exceeds a threshold.
     */
    public DefaultPromise(Executor executor) {
        Preconditions.checkNotNull(executor);
        this.executor = executor;
    }

    /**
     * 默认多线程执行listener回调, 想要所有listener回调在同一线程处理, 则需要使用{@link EventExecutor}, 或者使用者自定义实现
     */
    public DefaultPromise() {
        this(ForkJoinPool.commonPool());
    }

    @Override
    public Promise<V> success(V result) {
        if (success0(result)) {
            return this;
        }
        throw new IllegalStateException("complete already: " + this);
    }

    @Override
    public boolean trySuccess(V result) {
        return success0(result);
    }

    @Override
    public Promise<V> failure(Throwable cause) {
        if (failure0(cause)) {
            return this;
        }
        throw new IllegalStateException("complete already: " + this, cause);
    }

    @Override
    public boolean tryFailure(Throwable cause) {
        return failure0(cause);
    }

    @Override
    public boolean setUnCancellable() {
        if (RESULT_UPDATER.compareAndSet(this, null, UNCANCELLABLE)) {
            //将result set为UNCANCELLABLE实例
            return true;
        }
        Object result = this.result;
        return !isDone0(result) || !isCancelled0(result);
    }

    @Override
    public boolean isSuccess() {
        Object result = this.result;
        return result != null && result != UNCANCELLABLE && !(result instanceof CauseHolder);
    }

    @Override
    public boolean isCancellable() {
        //result有值都可视为Promise complete
        return result == null;
    }

    @Override
    public Throwable cause() {
        return cause0(result);
    }

    @Override
    public Promise<V> addListener(PromiseListener<? extends Promise<? super V>> listener) {
        Preconditions.checkNotNull(listener);

        synchronized (this) {
            addListener0(listener);
        }

        if (isDone()) {
            notifyListeners();
        }

        return this;
    }

    @Override
    public Promise<V> addListeners(PromiseListener<? extends Promise<? super V>>... listeners) {
        Preconditions.checkNotNull(listeners);

        synchronized (this) {
            for (PromiseListener<? extends Promise<? super V>> listener : listeners) {
                if (listener == null) {
                    break;
                }
                addListener0(listener);
            }
        }

        if (isDone()) {
            notifyListeners();
        }

        return this;
    }

    @Override
    public Promise<V> removeListener(PromiseListener<? extends Promise<? super V>> listener) {
        Preconditions.checkNotNull(listener);

        synchronized (this) {
            removeListener0(listener);
        }

        return this;
    }

    @Override
    public Promise<V> removeListeners(PromiseListener<? extends Promise<? super V>>... listeners) {
        Preconditions.checkNotNull(listeners);

        synchronized (this) {
            for (PromiseListener<? extends Promise<? super V>> listener : listeners) {
                if (listener == null) {
                    break;
                }
                removeListener0(listener);
            }
        }

        return this;
    }

    @Override
    public Promise<V> await() throws InterruptedException {
        if (isDone()) {
            return this;
        }

        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        checkDeadLock();

        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    wait();
                } finally {
                    decWaiters();
                }
            }
        }
        return this;
    }

    @Override
    public Promise<V> awaitUninterruptibly() {
        if (isDone()) {
            return this;
        }

        checkDeadLock();

        boolean interrupted = false;
        synchronized (this) {
            while (!isDone()) {
                incWaiters();
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Interrupted while waiting.
                    interrupted = true;
                } finally {
                    decWaiters();
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        return this;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    @Override
    public boolean await(long timeoutMillis) throws InterruptedException {
        return await0(MILLISECONDS.toNanos(timeoutMillis), true);
    }

    @Override
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        try {
            return await0(unit.toNanos(timeout), false);
        } catch (InterruptedException e) {
            // Should not be raised at all.
            throw new InternalError();
        }
    }

    @Override
    public boolean awaitUninterruptibly(long timeoutMillis) {
        try {
            return await0(MILLISECONDS.toNanos(timeoutMillis), false);
        } catch (InterruptedException e) {
            // Should not be raised at all.
            throw new InternalError();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public V getNow() {
        Object result = this.result;
        if (result instanceof CauseHolder || result == SUCCESS || result == UNCANCELLABLE) {
            return null;
        }
        return (V) result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get() throws InterruptedException, ExecutionException {
        Object result = this.result;
        if (!isDone0(result)) {
            await();
            result = this.result;
        }
        if (result == SUCCESS || result == UNCANCELLABLE) {
            return null;
        }
        Throwable cause = cause0(result);
        if (cause == null) {
            return (V) result;
        }
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Object result = this.result;
        if (!isDone0(result)) {
            if (!await(timeout, unit)) {
                throw new TimeoutException();
            }
            result = this.result;
        }
        if (result == SUCCESS || result == UNCANCELLABLE) {
            return null;
        }
        Throwable cause = cause0(result);
        if (cause == null) {
            return (V) result;
        }
        if (cause instanceof CancellationException) {
            throw (CancellationException) cause;
        }
        throw new ExecutionException(cause);
    }

    /**
     * {@inheritDoc}
     *
     * @param mayInterruptIfRunning this value has no effect in this implementation.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (RESULT_UPDATER.compareAndSet(this, null, CANCELLATION_CAUSE_HOLDER)) {
            if (checkNotifyWaiters()) {
                notifyListeners();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled0(result);
    }

    @Override
    public boolean isDone() {
        return isDone0(result);
    }

    @Override
    public Promise<V> sync() throws InterruptedException {
        await();
        rethrowIfFailed();
        return this;
    }

    @Override
    public Promise<V> syncUninterruptibly() {
        awaitUninterruptibly();
        rethrowIfFailed();
        return this;
    }

    @Override
    public String toString() {
        return toStringBuilder().toString();
    }

    protected StringBuilder toStringBuilder() {
        StringBuilder buf = new StringBuilder(64)
                .append(getClass().getSimpleName())
                .append('@')
                .append(Integer.toHexString(hashCode()));

        Object result = this.result;
        if (result == SUCCESS) {
            buf.append("(success)");
        } else if (result == UNCANCELLABLE) {
            buf.append("(unCancellable)");
        } else if (result instanceof CauseHolder) {
            buf.append("(failure: ")
                    .append(((CauseHolder) result).cause)
                    .append(')');
        } else if (result != null) {
            buf.append("(success: ")
                    .append(result)
                    .append(')');
        } else {
            buf.append("(incomplete)");
        }

        return buf;
    }

    /**
     * 针对{@link EventExecutor}做死锁检查
     */
    protected void checkDeadLock() {
        Executor executor = executor();
        if (Objects.nonNull(executor) && executor instanceof EventExecutor && ((EventExecutor) executor).isInEventLoop()) {
            throw new BlockingOperationException(toString());
        }
    }

    /**
     * 触发listener
     */
    private void notifyListeners() {
        Executor executor = executor();
        if (executor instanceof EventExecutor) {
            EventExecutor eventExecutor = (EventExecutor) executor;
            if (eventExecutor.isInEventLoop()) {
                //如果在eventExecutor下执行则需要检查, 调用栈深度
                InternalThreadLocalMap threadLocals = InternalThreadLocalMap.get();
                int stackDepth = threadLocals.promiseListenerStackDepth;
                if (stackDepth < MAX_LISTENER_STACK_DEPTH) {
                    threadLocals.promiseListenerStackDepth = stackDepth + 1;
                    try {
                        notifyListenersNow();
                    } finally {
                        threadLocals.promiseListenerStackDepth = stackDepth;
                    }
                    return;
                }
            }
        }

        safeExecute(executor, this::notifyListenersNow);
    }

    /**
     * 马上触发listener
     */
    private void notifyListenersNow() {
        Object listeners;
        synchronized (this) {
            // Only proceed if there are listeners to notify and we are not already notifying listeners.
            //当有listeners可触发并且没有其他线程在触发listener才允许触发listener
            if (notifyingListeners || this.listeners == null) {
                return;
            }
            notifyingListeners = true;
            //remove
            listeners = this.listeners;
            this.listeners = null;
        }
        for (; ; ) {
            if (listeners instanceof DefaultPromiseListeners) {
                notifyListeners0((DefaultPromiseListeners) listeners);
            } else {
                notifyListener0((PromiseListener<?>) listeners);
            }
            synchronized (this) {
                if (this.listeners == null) {
                    // Nothing can throw from within this method, so setting notifyingListeners back to false does not
                    // need to be in a finally block.
                    //reset状态
                    notifyingListeners = false;
                    return;
                }
                //如果期间有添加listener, 则继续触发
                listeners = this.listeners;
                this.listeners = null;
            }
        }
    }

    /**
     * 批量触发listener
     */
    private void notifyListeners0(DefaultPromiseListeners listeners) {
        PromiseListener<?>[] a = listeners.listeners();
        int size = listeners.size();
        for (int i = 0; i < size; i++) {
            notifyListener0(a[i]);
        }
    }

    /**
     * 添加listener, 如果存在多个, 会自动转换为{@link DefaultPromiseListeners}
     * 在synchronized下操作
     */
    private void addListener0(PromiseListener<? extends Promise<? super V>> listener) {
        if (listeners == null) {
            listeners = listener;
        } else if (listeners instanceof DefaultPromiseListeners) {
            ((DefaultPromiseListeners) listeners).add(listener);
        } else {
            listeners = new DefaultPromiseListeners((PromiseListener<?>) listeners, listener);
        }
    }

    /**
     * 如果存在多个, 则{@link DefaultPromiseListeners#remove(PromiseListener)}
     * 在synchronized下操作
     */
    private void removeListener0(PromiseListener<? extends Promise<? super V>> listener) {
        if (listeners instanceof DefaultPromiseListeners) {
            ((DefaultPromiseListeners) listeners).remove(listener);
        } else if (listeners == listener) {
            listeners = null;
        }
    }

    /**
     * Promise complete success通用逻辑
     */
    private boolean success0(V result) {
        //如果结果本来就是null, 则set为SUCCESS实例
        return setValue0(result == null ? SUCCESS : result);
    }

    /**
     * Promise complete failure通用逻辑
     */
    private boolean failure0(Throwable cause) {
        Preconditions.checkNotNull(cause);
        //使用CauseHolder包装异常, 易于区分异常和正常的操作结果
        return setValue0(new CauseHolder(cause));
    }

    /**
     * 更新{@link #result}通用逻辑
     */
    private boolean setValue0(Object objResult) {
        if (RESULT_UPDATER.compareAndSet(this, null, objResult) ||
                RESULT_UPDATER.compareAndSet(this, UNCANCELLABLE, objResult)) {
            //null和UNCANCELLABLE实例下, 都是允许更新result值的
            //
            if (checkNotifyWaiters()) {
                notifyListeners();
            }
            return true;
        }
        return false;
    }

    /**
     * Check if there are any waiters and if so notify these.
     *
     * @return {@code true} if there are any listeners attached to the promise, {@code false} otherwise.
     * <p>
     * 如果有waiter或者listeners, 则唤醒或者触发
     * 此处需要加锁, 保证一致性
     */
    private synchronized boolean checkNotifyWaiters() {
        if (waiters > 0) {
            notifyAll();
        }
        return listeners != null;
    }

    /**
     * 增加waiter数量
     * 在synchronized下操作
     */
    private void incWaiters() {
        if (waiters == Short.MAX_VALUE) {
            //最多允许32767个waiter
            throw new IllegalStateException("too many waiters: " + this);
        }
        ++waiters;
    }

    /**
     * 减少waiter数量
     * 在synchronized下操作
     */
    private void decWaiters() {
        --waiters;
    }

    /**
     * 如果操作异常退出, 则抛出异常
     *
     * @see #sync()
     * @see #syncUninterruptibly()
     */
    private void rethrowIfFailed() {
        Throwable cause = cause();
        if (cause == null) {
            return;
        }

        ExceptionUtils.throwExt(cause);
    }

    /**
     * 超时await统一逻辑
     */
    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        if (isDone()) {
            return true;
        }

        if (timeoutNanos <= 0) {
            return isDone();
        }

        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }

        checkDeadLock();

        long startTime = System.nanoTime();
        long waitTime = timeoutNanos;
        boolean interrupted = false;
        try {
            for (; ; ) {
                synchronized (this) {
                    //类似自旋, 一段时间唤醒并检查是否complete
                    if (isDone()) {
                        return true;
                    }
                    incWaiters();
                    try {
                        //等待
                        wait(waitTime / 1000000, (int) (waitTime % 1000000));
                    } catch (InterruptedException e) {
                        if (interruptable) {
                            throw e;
                        } else {
                            interrupted = true;
                        }
                    } finally {
                        decWaiters();
                    }
                }
                if (isDone()) {
                    return true;
                } else {
                    //计算下一次wait time
                    waitTime = timeoutNanos - (System.nanoTime() - startTime);
                    if (waitTime <= 0) {
                        return isDone();
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Get the executor used to notify listeners when this promise is complete.
     * <p>
     * It is assumed this executor will protect against {@link StackOverflowError} exceptions.
     * The executor may be used to avoid {@link StackOverflowError} by executing a {@link Runnable} if the stack
     * depth exceeds a threshold.
     *
     * @return The executor used to notify listeners when this promise is complete.
     */
    protected Executor executor() {
        return executor;
    }

    /**
     * 执行task
     */
    protected void safeExecute(Executor executor, Runnable task) {
        try {
            executor.execute(task);
        } catch (Throwable t) {
            log.error("failed to submit a listener notification task.", t);
        }
    }

    /**
     * 获取Promise失败异常原因
     */
    private Throwable cause0(Object result) {
        if (!(result instanceof CauseHolder)) {
            //非异常complete
            return null;
        }
        if (result == CANCELLATION_CAUSE_HOLDER) {
            //被取消
            CancellationException ce = new LeanCancellationException();
            if (RESULT_UPDATER.compareAndSet(this, CANCELLATION_CAUSE_HOLDER, new CauseHolder(ce))) {
                //替换更加详细的异常
                return ce;
            }
            result = this.result;
        }
        return ((CauseHolder) result).cause;
    }

    /**
     * 判断Promise result是否被取消通用逻辑
     */
    private boolean isCancelled0(Object result) {
        return result instanceof CauseHolder && ((CauseHolder) result).cause instanceof CancellationException;
    }

    /**
     * 判断Promise result是否complete通用逻辑
     */
    private boolean isDone0(Object result) {
        return result != null && result != UNCANCELLABLE;
    }

    /**
     * 触发listener
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void notifyListener0(PromiseListener l) {
        try {
            l.onComplete(this);
        } catch (Throwable t) {
            log.warn("an exception was thrown by " + l.getClass().getName() + ".onComplete()", t);
        }
    }

    //--------------------------------------------------------------------------------------------------

    /** 持有的异常的对象 */
    private static final class CauseHolder {
        private final Throwable cause;

        CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }

    /**
     * 有精准堆栈信息的Promise取消异常
     */
    private static final class LeanCancellationException extends CancellationException {
        private static final long serialVersionUID = 2794674970981187807L;

        // Suppress a warning since the method doesn't need synchronization
        @Override
        public Throwable fillInStackTrace() {   // lgtm[java/non-sync-override]
            setStackTrace(CANCELLATION_STACK);
            return this;
        }

        @Override
        public String toString() {
            return CancellationException.class.getName();
        }
    }

    /**
     * 缺乏堆栈信息的Promise取消异常
     */
    private static final class StacklessCancellationException extends CancellationException {

        private static final long serialVersionUID = -2974906711413716191L;

        private StacklessCancellationException() {
        }

        // Override fillInStackTrace() so we not populate the backtrace via a native call and so leak the
        // Classloader.
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

        static StacklessCancellationException newInstance(Class<?> clazz, String method) {
            StacklessCancellationException exception = new StacklessCancellationException();
            exception.setStackTrace(new StackTraceElement[]{new StackTraceElement(clazz.getName(), method, null, -1)});
            return exception;
        }
    }
}
