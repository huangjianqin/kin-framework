package org.kin.framework.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Extend array list to add peek/poll first/last element.
 * <p>
 * 基于{@link ArrayList}实现的双向数组
 * <p>
 * Forked from <a href="https://github.com/sofastack/sofa-jraft">SOFAJRaft</a>.
 *
 * @author huangjianqin
 * @date 2021/11/5
 */
public class ArrayDeque<E> extends ArrayList<E> {
    private static final long serialVersionUID = -7562494378488599756L;

    /**
     * Get the first element of list.
     * <p>
     * 取head element
     */
    public static <E> E peekFirst(List<E> list) {
        return list.get(0);
    }

    /**
     * Remove the first element from list and return it.
     * <p>
     * 移除head element
     */
    public static <E> E pollFirst(List<E> list) {
        return list.remove(0);
    }

    /**
     * Get the last element of list.
     * <p>
     * 取tail element
     */
    public static <E> E peekLast(List<E> list) {
        return list.get(list.size() - 1);
    }

    /**
     * Remove the last element from list and return it.
     * <p>
     * 移除tail element
     */
    public static <E> E pollLast(List<E> list) {
        return list.remove(list.size() - 1);
    }

    /**
     * Get the first element of list.
     * <p>
     * 取head element
     */
    public E peekFirst() {
        return peekFirst(this);
    }

    /**
     * Get the last element of list.
     * <p>
     * 取tail element
     */
    public E peekLast() {
        return peekLast(this);
    }

    /**
     * Remove the first element from list and return it.
     * <p>
     * 移除head element
     */
    public E pollFirst() {
        return pollFirst(this);
    }

    /**
     * Remove the last element from list and return it.
     * <p>
     * 移除tail element
     */
    public E pollLast() {
        return pollLast(this);
    }

    /**
     * Expose this methods so we not need to create a new subList just to
     * remove a range of elements.
     * <p>
     * Removes from this deque all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the deque by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     * <p>
     * 移除[fromIndex, toIndex)的elements, 这里verride, 是因为不想使用subList
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} or
     *                                   {@code toIndex} is out of range
     *                                   ({@code fromIndex < 0 ||
     *                                   fromIndex >= size() ||
     *                                   toIndex > size() ||
     *                                   toIndex < fromIndex})
     */
    @Override
    public void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
    }
}