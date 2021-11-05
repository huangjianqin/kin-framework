/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kin.framework.pool;

import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Light-weight object pool based on a thread-local stack.
 * <p>
 * Forked from <a href="https://github.com/sofastack/sofa-jraft">SOFAJRaft</a>.
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @param <T> the type of the pooled object
 * @author huangjianqin
 * @date 2021/11/3
 */
public abstract class Recycler<T> {
    private static final Logger log = LoggerFactory.getLogger(Recycler.class);

    /** {@link WeakOrderQueue#id}生成, 可简单理解为线程id */
    private static final AtomicInteger idGenerator = new AtomicInteger(Integer.MIN_VALUE);

    /** 0, 标识{@link ObjectPool.Handle}已回收到{@link Stack} */
    private static final int OWN_THREAD_ID = idGenerator.getAndIncrement();
    /** 默认每个线程最大对象数是4096 */
    private static final int DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD = 4 * 1024;
    /** 应用全局每个线程最大对象数, 可配置 */
    private static final int MAX_CAPACITY_PER_THREAD;
    /** 初始对象数 */
    private static final int INITIAL_CAPACITY;

    static {
        //初始化
        int maxCapacityPerThread = SysUtils.getIntSysProperty("kin.framework.recycler.maxCapacityPerThread", DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD);
        if (maxCapacityPerThread < 0) {
            maxCapacityPerThread = DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD;
        }

        MAX_CAPACITY_PER_THREAD = maxCapacityPerThread;

        log.info("-Dkin.framework.maxCapacityPerThread: {}.", MAX_CAPACITY_PER_THREAD == 0 ? "disabled" : MAX_CAPACITY_PER_THREAD);

        INITIAL_CAPACITY = Math.min(MAX_CAPACITY_PER_THREAD, 256);
    }

    /** 每个线程最大对象数 */
    private final int maxCapacityPerThread;
    /** 本线程绑定的{@link Stack}实例 */
    private final ThreadLocal<Stack<T>> threadLocal = new ThreadLocal<Stack<T>>() {

        @Override
        protected Stack<T> initialValue() {
            return new Stack<>(Recycler.this, Thread.currentThread(), maxCapacityPerThread);
        }
    };

    protected Recycler() {
        this(MAX_CAPACITY_PER_THREAD);
    }

    protected Recycler(int maxCapacityPerThread) {
        this.maxCapacityPerThread = Math.min(MAX_CAPACITY_PER_THREAD, Math.max(0, maxCapacityPerThread));
    }

    /**
     * 获取池化对象
     */
    @SuppressWarnings("unchecked")
    public final T get() {
        if (maxCapacityPerThread == 0) {
            //容量为0, 则马上创建新对象
            return newObject(ObjectPool.NOOP_HANDLE);
        }
        //绑定本线程Stack
        Stack<T> stack = threadLocal.get();
        DefaultHandle handle = stack.pop();
        if (handle == null) {
            handle = stack.newHandle();
            handle.value = newObject(handle);
        }
        return (T) handle.value;
    }

    /**
     * 通用的回收对象接口
     */
    public final boolean recycle(T o, ObjectPool.Handle handle) {
        if (Objects.isNull(handle)) {
            return false;
        }

        if (handle == ObjectPool.NOOP_HANDLE) {
            return false;
        }

        DefaultHandle h = (DefaultHandle) handle;

        Stack<?> stack = h.stack;
        if (h.lastRecycledId != h.recycleId || stack == null) {
            throw new IllegalStateException("recycled already");
        }

        if (stack.parent != this) {
            return false;
        }
        if (o != h.value) {
            throw new IllegalArgumentException("o does not belong to handle");
        }
        h.recycle();
        return true;
    }

    /**
     * 实现类自定的创建池化对象方法
     */
    protected abstract T newObject(ObjectPool.Handle handle);

    /**
     * 获取本线程的{@link Stack}可分配池化对象容量
     */
    public final int threadLocalCapacity() {
        return threadLocal.get().elements.length;
    }

    /**
     * 获取本线程的{@link Stack}剩余容量
     */
    public final int threadLocalSize() {
        return threadLocal.get().size;
    }

    //-------------------------------------------
    static final class DefaultHandle implements ObjectPool.Handle {
        /** 回收的id, 从哪个queue回收就等于对应{@link WeakOrderQueue#id} */
        private int lastRecycledId;
        /**
         * 从{@link WeakOrderQueue}取回{@link Stack}时, 会更新为lastRecycledId.
         * 当recycleId == lastRecycledId时, 表示从{@link WeakOrderQueue}已取回{@link Stack}, 或者是分配出去了;
         * 当recycleId != lastRecycledId时, 表示跨线程回收到指定{@link WeakOrderQueue}
         */
        private int recycleId;
        /** 池化对象归属的{@link Stack}, 即分配时线程绑定的{@link Stack} */
        private Stack<?> stack;
        /** 池化对象 */
        private Object value;

        DefaultHandle(Stack<?> stack) {
            this.stack = stack;
        }

        @Override
        public void recycle() {
            Thread thread = Thread.currentThread();

            Stack<?> stack = this.stack;
            if (lastRecycledId != recycleId || stack == null) {
                throw new IllegalStateException("recycled already");
            }

            if (thread == stack.thread) {
                //当前线程是分配时线程, 则直接回收
                stack.push(this);
                return;
            }

            //当入queue, 等待需要(下次取池化对象)时才回收
            // we don't want to have a ref to the queue as the value in our weak map
            // so we null it out; to ensure there are no races with restoring it later
            // we impose a memory ordering here (no-op on x86)
            Map<Stack<?>, WeakOrderQueue> delayedRecycled = Recycler.delayedRecycled.get();
            WeakOrderQueue queue = delayedRecycled.get(stack);
            if (queue == null) {
                //!!!去掉了queue数量限制
                delayedRecycled.put(stack, queue = new WeakOrderQueue(stack, thread));
            }
            queue.add(this);
        }
    }

    /** 其余线程的{@link Stack}以及对应的回收queue, {@link WeakOrderQueue}, 所以是支持跨线程回收对象 */
    private static final ThreadLocal<Map<Stack<?>, WeakOrderQueue>> delayedRecycled = ThreadLocal.withInitial(WeakHashMap::new);

    /**
     * a queue that makes only moderate guarantees about visibility: items are seen in the correct order,
     * but we aren't absolutely guaranteed to ever see anything at all, thereby keeping the queue cheap to maintain
     * <p>
     * 在{@link Stack}里面新的{@link WeakOrderQueue}在linklist head
     */
    private static final class WeakOrderQueue {
        /** {@link Link}里面待收回的{@link DefaultHandle}数量 */
        private static final int LINK_CAPACITY = 16;

        /**
         * Let Link extend AtomicInteger for intrinsics. The Link itself will be used as writerIndex.
         * <p>
         * 本身充当writerIndex, 即收回开始index
         * 回收时, {@link Stack}只会操作head {@link Link}, 而回收时(特别是跨线程), {@link WeakOrderQueue}只会操作tail {@link Link}, 并且head {@link Link}和tail {@link Link}不是同一对象
         */
        @SuppressWarnings("serial")
        private static final class Link extends AtomicInteger {
            /** 待回收的池化对象 */
            private final DefaultHandle[] elements = new DefaultHandle[LINK_CAPACITY];
            /** 已取回{@link Stack}的index */
            private int readIndex;
            /** 下一{@link Link} */
            private Link next;
        }

        /** chain of data items */
        private Link head, tail;
        /** pointer to another queue of delayed items for the same stack */
        private WeakOrderQueue next;
        /** queue所属线程 */
        private final WeakReference<Thread> owner;
        /** queue id */
        private final int id = idGenerator.getAndIncrement();

        WeakOrderQueue(Stack<?> stack, Thread thread) {
            head = tail = new Link();
            owner = new WeakReference<>(thread);

            synchronized (stackLock(stack)) {
                next = stack.head;
                stack.head = this;
            }
        }

        private Object stackLock(Stack<?> stack) {
            return stack;
        }

        /**
         * 跨线程回收时调用
         */
        void add(DefaultHandle handle) {
            handle.lastRecycledId = id;

            Link tail = this.tail;
            int writeIndex;
            if ((writeIndex = tail.get()) == LINK_CAPACITY) {
                this.tail = tail = tail.next = new Link();
                writeIndex = tail.get();
            }
            tail.elements[writeIndex] = handle;
            handle.stack = null;
            // we lazy set to ensure that setting stack to null appears before we unnull it in the owning thread;
            // this also means we guarantee visibility of an element in the queue if we see the index updated
            tail.lazySet(writeIndex + 1);
        }

        /**
         * 是否仍然有对象未取回, 适用于该queue绑定的线程被回收了, 那么queue tail link没有线程操作, stack去取回是安全的
         */
        boolean hasFinalData() {
            return tail.readIndex != tail.get();
        }

        /**
         * transfer as many items as we can from this queue to the stack, returning true if any were transferred
         * 只取回一个{@link Link}
         */
        boolean transfer(Stack<?> dst) {
            Link head = this.head;
            if (head == null) {
                //没有可以取回的对象
                return false;
            }

            if (head.readIndex == LINK_CAPACITY) {
                //当前Link已取完, 尝试取下一个
                if (head.next == null) {
                    //没有下一个
                    return false;
                }
                //更新linklist head
                this.head = head = head.next;
            }

            //开始取回的index
            int srcStart = head.readIndex;
            //最大取回的index
            int srcEnd = head.get();
            //取回数量
            int srcSize = srcEnd - srcStart;
            if (srcSize == 0) {
                //没有可以取回的对象
                return false;
            }

            //目标stack的剩余大小
            int dstSize = dst.size;
            //取回后, 理想的stack的剩余大小
            int expectedCapacity = dstSize + srcSize;

            if (expectedCapacity > dst.elements.length) {
                //超过当前容量, 增加容量, 返回实际容量
                int actualCapacity = dst.increaseCapacity(expectedCapacity);
                //更新最大取回的index, 也就是只取回一部分
                srcEnd = Math.min(srcStart + actualCapacity - dstSize, srcEnd);
            }

            if (srcStart != srcEnd) {
                //开始取回
                DefaultHandle[] srcElems = head.elements;
                DefaultHandle[] dstElems = dst.elements;
                int newDstSize = dstSize;
                for (int i = srcStart; i < srcEnd; i++) {
                    DefaultHandle element = srcElems[i];
                    if (element.recycleId == 0) {
                        element.recycleId = element.lastRecycledId;
                    } else if (element.recycleId != element.lastRecycledId) {
                        throw new IllegalStateException("recycled already");
                    }
                    element.stack = dst;
                    dstElems[newDstSize++] = element;
                    srcElems[i] = null;
                }
                dst.size = newDstSize;

                if (srcEnd == LINK_CAPACITY && head.next != null) {
                    //取完一整个Link, 则去掉并更新linklist head
                    this.head = head.next;
                }

                head.readIndex = srcEnd;
                return true;
            } else {
                // The destination stack is full already.
                return false;
            }
        }
    }

    /**
     * 每个线程绑定的对象池
     */
    static final class Stack<T> {
        /*
         * we keep a queue of per-thread queues, which is appended to once only, each time a new thread other
         * than the stack owner recycles: when we run out of items in our stack we iterate this collection
         * to scavenge those that can be reused. this permits us to incur minimal thread synchronisation whilst
         * still recycling all items.
         */
        /** 所属{@link Recycler} */
        final Recycler<T> parent;
        /** 绑定的线程 */
        final Thread thread;
        /** user可取的池化对象数组 */
        private DefaultHandle[] elements;
        /** 最大容量 */
        private final int maxCapacity;
        /** 当前池大小 */
        private int size;

        /** 其余线程回收该{@link Stack}分配出去的对象队列, linklist */
        private volatile WeakOrderQueue head;
        /** 当前可操作取回对象的{@link WeakOrderQueue}, 上次取回对象的{@link WeakOrderQueue} */
        private WeakOrderQueue cursor, prev;

        Stack(Recycler<T> parent, Thread thread, int maxCapacity) {
            this.parent = parent;
            this.thread = thread;
            this.maxCapacity = maxCapacity;
            elements = new DefaultHandle[Math.min(INITIAL_CAPACITY, maxCapacity)];
        }

        /**
         * 扩容
         */
        int increaseCapacity(int expectedCapacity) {
            int newCapacity = elements.length;
            int maxCapacity = this.maxCapacity;
            do {
                newCapacity <<= 1;
            } while (newCapacity < expectedCapacity && newCapacity < maxCapacity);

            newCapacity = Math.min(newCapacity, maxCapacity);
            if (newCapacity != elements.length) {
                elements = Arrays.copyOf(elements, newCapacity);
            }

            return newCapacity;
        }

        /**
         * user拿池化对象
         */
        DefaultHandle pop() {
            int size = this.size;
            if (size == 0) {
                if (!scavenge()) {
                    return null;
                }
                size = this.size;
            }
            size--;
            DefaultHandle ret = elements[size];
            if (ret.lastRecycledId != ret.recycleId) {
                throw new IllegalStateException("recycled multiple times");
            }
            ret.recycleId = 0;
            ret.lastRecycledId = 0;
            this.size = size;
            return ret;
        }

        /**
         * 从{@link WeakOrderQueue}取出分配出去的对象, 取回在其余线程回收的对象
         */
        private boolean scavenge() {
            // continue an existing scavenge, if any
            if (scavengeSome()) {
                return true;
            }

            //重置, 下次从head queue开始取回
            // reset our scavenge cursor
            prev = null;
            cursor = head;
            return false;
        }

        /**
         * 从{@link WeakOrderQueue}取出分配出去的对象, 取回在其余线程回收的对象
         */
        private boolean scavengeSome() {
            WeakOrderQueue cursor = this.cursor;
            if (cursor == null) {
                //到达linklist tail
                cursor = head;
                if (cursor == null) {
                    //没有queue
                    return false;
                }
            }

            boolean success = false;
            WeakOrderQueue prev = this.prev;
            do {
                if (cursor.transfer(this)) {
                    //当前指向的queue, 取回完成
                    success = true;
                    //结束, 下次还是从这个queue里面取回分配出去的对象
                    break;
                }

                //取回失败, 要不stack满了, 要不没有对象可取, 则
                WeakOrderQueue next = cursor.next;
                if (cursor.owner.get() == null) {
                    //1.如果queue绑定的线程被回收了, 则马上取回该queue的对象
                    // If the thread associated with the queue is gone, unlink it, after
                    // performing a volatile read to confirm there is no data left to collect.
                    // We never unlink the first queue, as we don't want to synchronize on updating the head.
                    if (cursor.hasFinalData()) {
                        //有数据未取回
                        for (; ; ) {
                            //不断尝试取回
                            if (cursor.transfer(this)) {
                                success = true;
                            } else {
                                break;
                            }
                        }
                    }
                    if (prev != null) {
                        //去掉该queue
                        prev.next = next;
                    }
                } else {
                    //2. 尝试从下一个queue取回
                    prev = cursor;
                }

                //指向next
                cursor = next;

            } while (cursor != null && !success);
            //update
            this.prev = prev;
            this.cursor = cursor;
            return success;
        }

        /**
         * 取回分配出去的对象
         * 只适用于在{@link Stack#thread}线程下回收时调用
         */
        void push(DefaultHandle item) {
            if ((item.recycleId | item.lastRecycledId) != 0) {
                /*
                 * 同线程 -> recycleId==lastRecycledId
                 * 跨线程 -> recycleId!=lastRecycledId
                 */
                throw new IllegalStateException("recycled already");
            }
            item.recycleId = item.lastRecycledId = OWN_THREAD_ID;

            int size = this.size;
            if (size >= maxCapacity) {
                // Hit the maximum capacity - drop the possibly youngest object.
                //最大容量, 则抛弃
                return;
            }
            if (size == elements.length) {
                //扩容
                elements = Arrays.copyOf(elements, Math.min(size << 1, maxCapacity));
            }

            elements[size] = item;
            this.size = size + 1;
        }

        DefaultHandle newHandle() {
            return new DefaultHandle(this);
        }
    }
}
