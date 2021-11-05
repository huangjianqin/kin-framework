/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kin.framework.collection;

import org.kin.framework.pool.AbstractPooledObject;
import org.kin.framework.pool.ObjectPool;
import org.kin.framework.pool.Recyclable;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * A list implementation based on segments. Only supports removing elements from start or end.
 * The list keep the elements in a segment list, every segment contains at most 128 elements.
 * <p>
 * [segment, segment, segment ...]
 * /                 |                    \
 * segment             segment              segment
 * [0, 1 ... 127]    [128, 129 ... 255]    [256, 257 ... 383]
 * <p>
 * Forked from <a href="https://github.com/sofastack/sofa-jraft">SOFAJRaft</a>.
 *
 * @author huangjianqin
 * @date 2021/11/4
 */
public class SegmentList<T> {
    /** 用于计算index是落在哪个{@link Segment} */
    private static final int SEGMENT_SHIFT = 7;
    /** {@link Segment}最大容量 */
    private static final int SEGMENT_SIZE = 2 << (SEGMENT_SHIFT - 1);
    /** {@link Segment}双向队列 */
    private final ArrayDeque<Segment<T>> segments;
    /** 当前大小 */
    private int size;
    /** 缓存第一个{@link Segment#offset} */
    private int firstOffset;
    /** 标识{@link Segment}对象是否复用 */
    private final boolean recycleSegment;

    public SegmentList() {
        this(false);
    }

    /**
     * Create a new SegmentList
     *
     * @param recycleSegment true to enable recycling segment, only effective in same thread.
     */
    public SegmentList(boolean recycleSegment) {
        segments = new ArrayDeque<>();
        size = 0;
        firstOffset = 0;
        this.recycleSegment = recycleSegment;
    }

    /**
     * 取指定index的element
     */
    public T get(int index) {
        index += firstOffset;
        return segments.get(index >> SEGMENT_SHIFT).get(index & (SEGMENT_SIZE - 1));
    }

    /**
     * 取tail element
     */
    public T peekLast() {
        Segment<T> lastSeg = getLast();
        return lastSeg == null ? null : lastSeg.peekLast();
    }

    /**
     * 取最后一个元素
     */
    public T peekFirst() {
        Segment<T> firstSeg = getFirst();
        return firstSeg == null ? null : firstSeg.peekFirst();
    }

    /**
     * 取head {@link Segment}
     */
    private Segment<T> getFirst() {
        if (!segments.isEmpty()) {
            return segments.peekFirst();
        }
        return null;
    }

    /**
     * add element
     */
    @SuppressWarnings("unchecked")
    public void add(T e) {
        //取tail segment
        Segment<T> lastSeg = getLast();
        if (lastSeg == null || lastSeg.isFull()) {
            lastSeg = (Segment<T>) Segment.newInstance(recycleSegment);
            segments.add(lastSeg);
        }
        lastSeg.add(e);
        size++;
    }

    /**
     * 取tail {@link Segment}
     */
    private Segment<T> getLast() {
        if (!segments.isEmpty()) {
            return segments.get(segments.size() - 1);
        }
        return null;
    }

    /**
     * list size
     */
    public int size() {
        return size;
    }

    /**
     * list segment size
     */
    public int getSegmentSize() {
        return segments.size();
    }

    /**
     * list是否empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 从head开始移除, 直到不满足条件
     */
    public void removeFromFirstWhen(Predicate<T> predicate) {
        Segment<T> firstSeg = getFirst();
        while (true) {
            if (firstSeg == null) {
                firstOffset = size = 0;
                return;
            }
            int removed = firstSeg.removeFromFirstWhen(predicate);
            if (removed == 0) {
                break;
            }
            size -= removed;
            firstOffset = firstSeg.offset;
            if (firstSeg.isEmpty()) {
                Recyclable.recycle(segments.pollFirst());
                firstSeg = getFirst();
                firstOffset = 0;
            }
        }
    }

    /**
     * clear list
     */
    public void clear() {
        while (!segments.isEmpty()) {
            Recyclable.recycle(segments.pollFirst());
        }
        size = firstOffset = 0;
    }

    /**
     * 从tail开始移除, 直到不满足条件
     */
    public void removeFromLastWhen(Predicate<T> predicate) {
        Segment<T> lastSeg = getLast();
        while (true) {
            if (lastSeg == null) {
                firstOffset = size = 0;
                return;
            }
            int removed = lastSeg.removeFromLastWhen(predicate);
            if (removed == 0) {
                break;
            }
            size -= removed;
            if (lastSeg.isEmpty()) {
                Recyclable.recycle(segments.pollLast());
                lastSeg = getLast();
            }
        }
    }

    /**
     * 从head开始移除, 直到toIndex(exclusive)
     * Shifts any succeeding elements to the left (reduces their index).
     */
    public void removeFromFirst(int toIndex) {
        //真正的index
        int alignedIndex = toIndex + firstOffset;
        //对应segment index
        int toSegmentIndex = alignedIndex >> SEGMENT_SHIFT;
        //对应segment里面的elements index
        int toIndexInSeg = alignedIndex & (SEGMENT_SIZE - 1);

        if (toSegmentIndex > 0) {
            //移除前面的segment
            //回收直接移除的segment
            for (int i = 0; i < toSegmentIndex; i++) {
                Recyclable.recycle(segments.get(i));
            }
            segments.removeRange(0, toSegmentIndex);
            size -= ((toSegmentIndex << SEGMENT_SHIFT) - firstOffset);
        }

        Segment<T> firstSeg = getFirst();
        if (firstSeg != null) {
            size -= firstSeg.removeFromFirst(toIndexInSeg);
            firstOffset = firstSeg.offset;
            if (firstSeg.isEmpty()) {
                Recyclable.recycle(segments.pollFirst());
                firstOffset = 0;
            }
        } else {
            firstOffset = size = 0;
        }
    }

    /**
     * 添加{@code collection}全部elements
     */
    @SuppressWarnings("unchecked")
    public void addAll(Collection<T> collection) {
        Object[] src = new Object[collection.size()];
        src = collection.toArray(src);

        int srcPos = 0;
        int srcSize = collection.size();

        Segment<T> lastSeg = getLast();
        while (srcPos < srcSize) {
            if (lastSeg == null || lastSeg.isFull()) {
                lastSeg = (Segment<T>) Segment.newInstance(recycleSegment);
                segments.add(lastSeg);
            }

            int len = Math.min(lastSeg.cap(), srcSize - srcPos);
            lastSeg.addAll(src, srcPos, len);
            srcPos += len;
            size += len;
        }
    }

    @Override
    public String toString() {
        return "SegmentList [segments=" + segments + ", size=" + size + ", firstOffset=" + firstOffset
                + "]";
    }

    //-------------------------------------------------

    /**
     * A recyclable segment
     */
    private final static class Segment<T> extends AbstractPooledObject {
        /** 每个线程可复用128的{@link Segment}实例 */
        private static final ObjectPool<Segment<?>> SEGMENT_OBJECT_POOL = ObjectPool.newPool(16_384 / SEGMENT_SIZE, Segment::new);

        /**
         * 构建{@link Segment}实例
         *
         * @param recycleSegment 是否取复用{@link Segment}实例
         * @return {@link Segment}实例
         */
        public static Segment<?> newInstance(boolean recycleSegment) {
            if (recycleSegment) {
                return SEGMENT_OBJECT_POOL.get();
            } else {
                return new Segment<>();
            }
        }

        /** 该分段存储的数据 */
        final T[] elements;
        /** start offset(inclusive) */
        int offset;
        /** end offset(exclusive) */
        int pos;

        Segment() {
            this(ObjectPool.NOOP_HANDLE);
        }

        @SuppressWarnings("unchecked")
        Segment(ObjectPool.Handle handle) {
            super(handle);
            elements = (T[]) new Object[SEGMENT_SIZE];
            pos = offset = 0;
        }

        /**
         * clear all elements
         */
        void clear() {
            pos = offset = 0;
            Arrays.fill(elements, null);
        }

        @Override
        protected void beforeRecycle() {
            clear();
        }

        /**
         * 剩余容量
         */
        int cap() {
            return SEGMENT_SIZE - pos;
        }

        /**
         * add array all elements
         */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        private void addAll(Object[] src, int srcPos, int len) {
            System.arraycopy(src, srcPos, elements, pos, len);
            pos += len;
        }

        /**
         * 是否满了
         */
        boolean isFull() {
            return pos == SEGMENT_SIZE;
        }

        /**
         * 是否空了
         */
        boolean isEmpty() {
            return size() == 0;
        }

        /**
         * add single element
         */
        void add(T e) {
            elements[pos++] = e;
        }

        /**
         * 获取指定index的element
         */
        T get(int index) {
            if (index >= pos || index < offset) {
                throw new IndexOutOfBoundsException("index=" + index + ", offset=" + offset + ", pos=" + pos);
            }
            return elements[index];
        }

        /**
         * 取最后一个element
         */
        T peekLast() {
            return get(pos - 1);
        }

        /**
         * 当前大小
         */
        int size() {
            return pos - offset;
        }

        /**
         * 取第一个element
         */
        T peekFirst() {
            return get(offset);
        }

        /**
         * 从tail开始移除, 直到不满足条件
         */
        int removeFromLastWhen(Predicate<T> predicate) {
            int removed = 0;
            for (int i = pos - 1; i >= offset; i--) {
                T e = elements[i];
                if (predicate.test(e)) {
                    elements[i] = null;
                    removed++;
                } else {
                    break;
                }
            }
            pos -= removed;
            return removed;
        }

        /**
         * 从head开始移除, 直到不满足条件
         */
        int removeFromFirstWhen(Predicate<T> predicate) {
            int removed = 0;
            for (int i = offset; i < pos; i++) {
                T e = elements[i];
                if (predicate.test(e)) {
                    elements[i] = null;
                    removed++;
                } else {
                    break;
                }
            }
            offset += removed;
            return removed;
        }

        /**
         * 从head开始移除, 直到toIndex(exclusive)
         */
        int removeFromFirst(int toIndex) {
            int removed = 0;
            for (int i = offset; i < Math.min(toIndex, pos); i++) {
                elements[i] = null;
                removed++;
            }
            offset += removed;
            return removed;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (int i = offset; i < pos; i++) {
                b.append(elements[i]);
                if (i != pos - 1) {
                    b.append(", ");
                }
            }
            return "Segment [elements=" + b + ", offset=" + offset + ", pos=" + pos + ", hashcode=" + hashCode() + "]";
        }

    }
}
