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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 更灵活的{@link Future}实现
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2021/11/10
 */
public interface Promise<V> extends Future<V> {
    /**
     * Marks this future as a success and notifies all
     * listeners.
     * <p>
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     * <p>
     * 标识该Promise complete并触发所有listeners, 如果已经 complete, 则会抛{@link IllegalStateException}
     */
    Promise<V> success(V result);

    /**
     * Marks this future as a success and notifies all
     * listeners.
     * <p>
     * 标识该Promise complete并触发所有listeners
     *
     * @return {@code true} if and only if successfully marked this future as
     * a success. Otherwise {@code false} because this future is
     * already marked as either a success or a failure.
     */
    boolean trySuccess(V result);

    /**
     * Marks this future as a failure and notifies all
     * listeners.
     * <p>
     * If it is success or failed already it will throw an {@link IllegalStateException}.
     * <p>
     * 标识该Promise异常失败并触发所有listeners, 如果已经 complete, 则会抛{@link IllegalStateException}
     */
    Promise<V> failure(Throwable cause);

    /**
     * Marks this future as a failure and notifies all
     * listeners.
     * <p>
     * 标识该Promise异常失败并触发所有listeners
     *
     * @return {@code true} if and only if successfully marked this future as
     * a failure. Otherwise {@code false} because this future is
     * already marked as either a success or a failure.
     */
    boolean tryFailure(Throwable cause);

    /**
     * Make this future impossible to cancel.
     * <p>
     * 设置该Promise不可能被取消
     *
     * @return {@code true} if and only if successfully marked this future as uncancellable or it is already done
     * without being cancelled.  {@code false} if this future has been cancelled already.
     */
    boolean setUnCancellable();

    /**
     * Returns {@code true} if and only if the I/O operation was completed
     * successfully.
     * <p>
     * 返回Promise是否成功
     */
    boolean isSuccess();

    /**
     * returns {@code true} if and only if the operation can be cancelled via {@link #cancel(boolean)}.
     * <p>
     * 返回Promise是否可以被取消
     */
    boolean isCancellable();

    /**
     * Returns the cause of the failed I/O operation if the I/O operation has
     * failed.
     * <p>
     * 返回Promise失败异常原因
     *
     * @return the cause of the failure.
     * {@code null} if succeeded or this future is not
     * completed yet.
     */
    Throwable cause();

    /**
     * Adds the specified listener to this future.  The
     * specified listener is notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listener is notified immediately.
     * <p>
     * 添加指定listener. 当Promise complete时, 该listener会被触发. 如果future已经 complete, 则马上触发
     */
    Promise<V> addListener(PromiseListener<? extends Promise<? super V>> listener);

    /**
     * Adds the specified listeners to this future.  The
     * specified listeners are notified when this future is
     * {@linkplain #isDone() done}.  If this future is already
     * completed, the specified listeners are notified immediately.
     * <p>
     * 添加指定listener. 当Promise complete时, 该listener会被触发. 如果future已经 complete, 则马上触发
     */
    Promise<V> addListeners(PromiseListener<? extends Promise<? super V>>... listeners);

    /**
     * Removes the first occurrence of the specified listener from this future.
     * The specified listener is no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listener is not associated with this future, this method
     * does nothing and returns silently.
     * <p>
     * 从该Promise移除第一个指定listener. 当然Promise complete时, 该listener并不会触发. 如果不存在指定listener, 则do nothing
     */
    Promise<V> removeListener(PromiseListener<? extends Promise<? super V>> listener);

    /**
     * Removes the first occurrence for each of the listeners from this future.
     * The specified listeners are no longer notified when this
     * future is {@linkplain #isDone() done}.  If the specified
     * listeners are not associated with this future, this method
     * does nothing and returns silently.
     * <p>
     * 从该Promise移除第一个指定listeners. 当然Promise complete时, 该listeners并不会触发. 如果不存在指定listeners, 则do nothing
     */
    Promise<V> removeListeners(PromiseListener<? extends Promise<? super V>>... listeners);

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     * <p>
     * 等待Promise complete, 如果Promise失败, 则同样抛出异常
     */
    Promise<V> sync() throws InterruptedException;

    /**
     * Waits for this future until it is done, and rethrows the cause of the failure if this future
     * failed.
     * <p>
     * 等待Promise complete, 如果Promise失败, 则同样抛出异常
     * 需要注意的是, 该方法调用不会被中断
     */
    Promise<V> syncUninterruptibly();

    /**
     * Waits for this future to be completed.
     * <p>
     * 等待Promise complete
     *
     * @throws InterruptedException if the current thread was interrupted
     */
    Promise<V> await() throws InterruptedException;

    /**
     * Waits for this future to be completed without
     * interruption.  This method catches an {@link InterruptedException} and
     * discards it silently.
     * <p>
     * 等待Promise complete
     * 需要注意的是, 该方法调用不会被中断
     */
    Promise<V> awaitUninterruptibly();

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     * <p>
     * 阻塞一段, 等待Promise complete
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     * @throws InterruptedException if the current thread was interrupted
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit.
     * <p>
     * 阻塞一段, 等待Promise complete
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     * @throws InterruptedException if the current thread was interrupted
     */
    boolean await(long timeoutMillis) throws InterruptedException;

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     * <p>
     * 阻塞一段, 等待Promise complete
     * 需要注意的是, 该方法调用不会被中断
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     */
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);

    /**
     * Waits for this future to be completed within the
     * specified time limit without interruption.  This method catches an
     * {@link InterruptedException} and discards it silently.
     * <p>
     * 阻塞一段, 等待Promise complete
     * 需要注意的是, 该方法调用不会被中断
     *
     * @return {@code true} if and only if the future was completed within
     * the specified time limit
     */
    boolean awaitUninterruptibly(long timeoutMillis);

    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     * <p>
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not rely on the returned {@code null} value.
     * <p>
     * 马上获取Promise complete结果, 如果Promise未 complete, 则返回null.
     */
    V getNow();
}
