package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.StringUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.framework.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * <p>
 * 参考kafka的TimingWheel实现, 延迟调度比较精准
 * 1. wheel timer bucket: 链表结构, O(1), 相比, {@link java.util.concurrent.DelayQueue}和{@link java.util.Timer}, O(log n)
 * 插入和删除调度任务的性能更高.
 * 2. {@link #newTimeout(TimerTask, long, TimeUnit)}马上插入wheel timer bucket
 * 3. 利用层级关系来解决大于一轮的延迟时间({@link WheelTimer#wheelSpan})调度问题. 该灵感来自于现实世界的时钟, 60s为1min, 60min为1h.
 * 具体实现逻辑¬是1层wheel timer是60 bucket | 1s/tick; 2层wheel timer是60 bucket | 1min/tick; 3层wheel timer是60 bucket | 1h/tick(n层, n越大, 层级越高).
 * 如果一个调度任务超过1层wheel timer最长延迟时间60s, 则会插入到更高层的wheel timer, 以此类推.
 * 4. 以bucket作为{@link DelayQueue}item, 并启动一个worker线程来推动{@link DelayQueue}, 达到调度延迟任务. 以bucket作为{@link DelayQueue}item,
 * 可以大大减少{@link DelayQueue}item数量, 以达到更高效的插入删除性能.
 *
 * @author huangjianqin
 * @date 2022/1/7
 */
@ThreadSafe
public class LevelWheelTimer implements Timer {
    private static final Logger log = LoggerFactory.getLogger(LevelWheelTimer.class);
    /** worker state updater */
    private static final AtomicIntegerFieldUpdater<LevelWheelTimer> WORKER_STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(LevelWheelTimer.class, "workerState");
    /** state-init */
    public static final int WORKER_STATE_INIT = 0;
    /** state-started */
    public static final int WORKER_STATE_STARTED = 1;
    /** state-shutdown */
    public static final int WORKER_STATE_SHUTDOWN = 2;
    /** {@link #timeoutMs}默认值 */
    public static final int DEFAULT_TIMEOUT_MS = 200;

    /** expire task handler */
    private final ExecutionContext executionContext;
    /** 底层时间轮 */
    private final WheelTimer wheelTimer;
    /** bucket 延迟队列 */
    private final DelayQueue<TimerTaskList> queue = new DelayQueue<>();
    /** 时间轮每次往前走的毫秒数 */
    private final int timeoutMs;
    /** 读写锁, 保证多线程添加延迟任务, 但是worker处理延迟任务时, 不能添加延迟任务 */
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
    /** 任务数统计 */
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    /** 0 - init, 1 - started, 2 - shutdown */
    private volatile int workerState;

    public LevelWheelTimer(long tickMs, int wheelSize) {
        this(tickMs, wheelSize, DEFAULT_TIMEOUT_MS);
    }

    public LevelWheelTimer(int parallelism, long tickMs, int wheelSize) {
        this(parallelism, tickMs, wheelSize, DEFAULT_TIMEOUT_MS);
    }

    public LevelWheelTimer(long tickMs, int wheelSize, int timeoutMs) {
        this(SysUtils.CPU_NUM / 2 + 1, tickMs, wheelSize, timeoutMs);
    }

    public LevelWheelTimer(int parallelism, long tickMs, int wheelSize, int timeoutMs) {
        this("levelWheelTimer-expire-handler", parallelism, tickMs, wheelSize, timeoutMs);
    }

    /**
     * @param executorName expire task handler name
     * @param parallelism  expire task handler num
     * @param tickMs       tick毫秒数
     * @param wheelSize    时间轮大小
     * @param timeoutMs    时间轮每次往前走的毫秒数
     */
    public LevelWheelTimer(String executorName, int parallelism, long tickMs, int wheelSize, int timeoutMs) {
        Preconditions.checkArgument(StringUtils.isNotBlank(executorName), "executorName must not be blank");
        Preconditions.checkArgument(parallelism > 0, "parallelism must be greater than 0");
        Preconditions.checkArgument(tickMs > 0, "tickMs must be greater than 0");
        Preconditions.checkArgument(wheelSize > 0, "wheelSize must be greater than 0");
        Preconditions.checkArgument(timeoutMs > 0, "timeoutMs must be greater than 0");
        executionContext = ExecutionContext.fix(parallelism + 1, executorName);
        wheelTimer = new WheelTimer(tickMs, wheelSize, TimeUtils.millisFromNanoTime(), taskCounter, queue);
        this.timeoutMs = timeoutMs;
    }

    /**
     * 启动worker, 推动{@link DelayQueue}里面有延迟任务的bucket
     */
    private void tryStartWorker() {
        if (WORKER_STATE_UPDATER.get(this) != WORKER_STATE_INIT) {
            //已经start
            return;
        }

        if (WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
            executionContext.execute(() -> {
                //worker逻辑
                while (WORKER_STATE_UPDATER.get(LevelWheelTimer.this) == WORKER_STATE_STARTED) {
                    //推动时间轮往前走
                    advanceClock(timeoutMs);
                }
            });
        }
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        if (WORKER_STATE_UPDATER.get(this) == WORKER_STATE_SHUTDOWN) {
            throw new IllegalStateException("timer is already stopped");
        }

        //读锁, 即允许多线程添加延迟队列, 但worker处理延迟任务时, 不能添加延迟任务
        readLock.lock();
        try {
            //创建timeout
            LevelWheelTimeout timeout = new LevelWheelTimeout(this, task);
            //添加到时间轮
            reAddEntryIfNotRunTask(new TimerTaskEntry(timeout, TimeUtils.millisFromNanoTime() + unit.toMillis(delay)));
            //尝试启动worker
            tryStartWorker();
            return timeout;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * tick时, 遍历bucket所有entry, 如果存在entry未过期, 则重新添加到时间轮(有可能降级到下一层的时间轮), 否者执行过期逻辑
     */
    private void reAddEntryIfNotRunTask(TimerTaskEntry entry) {
        if (wheelTimer.add(entry)) {
            //未过期or未取消
            return;
        }

        if (entry.isCancelled()) {
            //已取消
            return;
        }

        //已过期
        executionContext.execute(() -> {
            try {
                entry.timeout.task.run(entry.timeout);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("An exception was thrown by " + TimerTask.class.getSimpleName() + '.', e);
                }
            }
        });
    }

    /**
     * 时间轮往前走{@code timeoutMs}, 如果没有任何过期bucket触发, 则等待timeout并且do nothing
     */
    private boolean advanceClock(long timeoutMs) {
        TimerTaskList bucket = null;
        try {
            //等待队列中有满足时间触发的bucket
            bucket = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //do nothing
        }

        if (Objects.isNull(bucket)) {
            return false;
        }

        //写锁, 即允许多线程添加延迟队列, 但worker处理延迟任务时, 不能添加延迟任务
        writeLock.lock();
        try {
            //处理过期的bucket
            while (Objects.nonNull(bucket)) {
                //推动时间轮往前走
                wheelTimer.advanceClock(bucket.getExpiration());
                //清空bucket里面的延迟task, 并且对每个task再次调用reAddEntryIfNotRunTask()
                bucket.flush(this::reAddEntryIfNotRunTask);
                //如果还有过期的bucket, 则马上取出来处理
                bucket = queue.poll();
            }
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    /**
     * @return 当前等待中的延迟任务数
     */
    public int size() {
        return taskCounter.get();
    }

    @Override
    public Set<Timeout> stop() {
        if (!WORKER_STATE_UPDATER.compareAndSet(this, WORKER_STATE_STARTED, WORKER_STATE_SHUTDOWN)) {
            //重复stop
            throw new IllegalStateException("timer is already stopped");
        }

        executionContext.shutdown();

        //剩余未过期的task
        Set<Timeout> rest = new HashSet<>();

        //遍历所有不同层的WheelTimer
        WheelTimer wheelTimer = this.wheelTimer;
        while (Objects.nonNull(wheelTimer)) {
            //遍历所有bucket
            for (TimerTaskList bucket : wheelTimer.queue) {
                bucket.flush(e -> {
                    LevelWheelTimeout timeout = e.timeout;
                    timeout.cancel();
                    rest.add(timeout);
                });
            }
            wheelTimer = wheelTimer.overflowWheel;
        }
        return rest;
    }

    //-----------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 底层时间轮实现
     * 这里面用到的毫秒数并不是根据系统时间获得, 而是通过{@link System#nanoTime()}转换得来
     */
    @NotThreadSafe
    private static final class WheelTimer {
        /** 一bucket的时间, tick time */
        private final long tickMs;
        /** bucket数量 */
        private final int wheelSize;
        /** 开始时间 */
        private final long startMs;
        /** pending task统计 */
        private final AtomicInteger taskCounter;
        /** bucket delay queue */
        private final DelayQueue<TimerTaskList> queue;
        /** 时间轮时间长度 */
        private final long wheelSpan;
        /** bucket array */
        private final TimerTaskList[] buckets;

        /** 当前时间, rounding down to tickMs的倍数 */
        private long currentTime;
        /**
         * 上层时间轮
         * 动态根据需求创建, 所以需要synchronized + volatile
         */
        private volatile WheelTimer overflowWheel = null;

        public WheelTimer(long tickMs, int wheelSize, long startMs, AtomicInteger taskCounter, DelayQueue<TimerTaskList> queue) {
            this.tickMs = tickMs;
            this.wheelSize = wheelSize;
            this.startMs = startMs;
            this.taskCounter = taskCounter;
            this.queue = queue;
            this.wheelSpan = tickMs * wheelSize;
            this.buckets = new TimerTaskList[wheelSize];
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new TimerTaskList(this, taskCounter);
            }
            this.currentTime = startMs - (startMs % tickMs);
        }

        /**
         * 因为延迟任务延迟时间大于时间轮长度{@link #wheelSpan}, 故需要创建上层时间轮
         * 添加上层时间轮
         */
        private void addOverflowWheel() {
            synchronized (this) {
                //加锁, 防止线程竞争
                if (Objects.nonNull(overflowWheel)) {
                    return;
                }

                overflowWheel = new WheelTimer(wheelSpan, wheelSize, currentTime, taskCounter, queue);
            }
        }

        /**
         * 往时间轮添加task entry
         */
        public boolean add(TimerTaskEntry entry) {
            //过期时间
            long expirationMs = entry.expirationMs;

            if (entry.isCancelled()) {
                //cancelled
                return false;
            } else if (expirationMs < currentTime + tickMs) {
                //already expired
                return false;
            } else if (expirationMs < currentTime + wheelSpan) {
                //添加到对应bucket
                long virtualId = expirationMs / tickMs;
                TimerTaskList bucket = buckets[(int) virtualId % wheelSize];
                bucket.add(entry);

                //更新bucket expiration time
                if (bucket.setExpiration(virtualId * tickMs)) {
                    //如果更新成功, 则需要push到delay queue, 同一expiration time, 不会push到delay queue多次
                    queue.offer(bucket);
                }
                return true;
            } else {
                //大于时间轮长度 interval. 往上层时间轮添加task entry
                if (Objects.isNull(overflowWheel)) {
                    //不存在上层时间轮, 则创建
                    addOverflowWheel();
                }
                return overflowWheel.add(entry);
            }
        }

        /**
         * 时间轮往前走{@code timeoutMs}
         */
        public void advanceClock(long timeMs) {
            if (timeMs < currentTime + tickMs) {
                return;
            }

            //timeMs超过一bucket
            //更新currentTime, round down to tickMs的倍数
            currentTime = timeMs - (timeMs % tickMs);

            if (Objects.nonNull(overflowWheel)) {
                //如果有上层时间轮, 则推动上层时间轮往前走
                overflowWheel.advanceClock(currentTime);
            }
        }
    }

    /**
     * wheel timer bucket
     * 双向链表
     * root作为哨兵
     */
    @ThreadSafe
    private static final class TimerTaskList implements Delayed {
        /** bucket所在的{@link WheelTimer} */
        private final WheelTimer timer;
        /** pending task统计 */
        private final AtomicInteger taskCounter;
        /** 哨兵节点 */
        private final TimerTaskEntry root = new TimerTaskEntry(null, -1);
        /** bucket expiration time */
        private final AtomicLong expiration = new AtomicLong(-1);

        public TimerTaskList(WheelTimer timer, AtomicInteger taskCounter) {
            this.timer = timer;
            this.taskCounter = taskCounter;
            root.next = root;
            root.prev = root;
        }

        /**
         * 更新expiration time
         */
        private boolean setExpiration(long expirationMs) {
            return expiration.getAndSet(expirationMs) != expirationMs;
        }

        /**
         * 获取expiration time
         */
        public long getExpiration() {
            return expiration.get();
        }

        /**
         * 添加一timer task到该bucket
         */
        public void add(TimerTaskEntry entry) {
            boolean done = false;
            while (!done) {
                //这里用while是为了重试

                //如果该timer task entry在另外的list, 则先remove. 在synchronized块外面remove, 是为了避免死锁
                entry.remove();

                synchronized (this) {
                    synchronized (entry) {
                        if (Objects.nonNull(entry.list)) {
                            return;
                        }

                        //将timer task entry添加到bucket list tail
                        //root.prev指向tail entry
                        TimerTaskEntry tail = root.prev;
                        entry.next = root;
                        entry.prev = tail;
                        entry.list = this;

                        tail.next = entry;
                        root.prev = entry;
                        taskCounter.incrementAndGet();
                        done = true;
                    }
                }
            }
        }

        /**
         * 从bucket list移除指定{@code entry}
         */
        public void remove(TimerTaskEntry entry) {
            synchronized (this) {
                synchronized (entry) {
                    if (!entry.list.equals(this)) {
                        return;
                    }
                    entry.next.prev = entry.prev;
                    entry.prev.next = entry.next;

                    //clear
                    entry.next = null;
                    entry.prev = null;
                    entry.list = null;

                    taskCounter.decrementAndGet();
                }
            }
        }

        /**
         * 移除bucket list所有entries, 并对每一个entry执行{@code consumer}逻辑
         */
        public void flush(Consumer<TimerTaskEntry> consumer) {
            synchronized (this) {
                TimerTaskEntry head = root.next;
                while (!head.equals(root)) {
                    remove(head);
                    consumer.accept(head);
                    head = root.next;
                }
                expiration.set(-1);
            }
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(Math.max(getExpiration() - TimeUtils.millisFromNanoTime(), 0), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            TimerTaskList otherList = (TimerTaskList) o;
            return Long.compare(getExpiration(), otherList.getExpiration());
        }
    }

    /**
     * bucket list entry
     */
    private static final class TimerTaskEntry implements Comparator<TimerTaskEntry> {
        /** 封装延迟任务的timeout实例 */
        private final LevelWheelTimeout timeout;
        /** 过期时间 */
        private final long expirationMs;

        /** 该entry所在的bucket list, 如果null, 则是从bucket移除, 可能是过期, 可能是被取消 */
        private volatile TimerTaskList list;
        /** 下一entry, 则是从bucket移除, 可能是过期, 可能是被取消 */
        private TimerTaskEntry next;
        /** 上一entry, 则是从bucket移除, 可能是过期, 可能是被取消 */
        private TimerTaskEntry prev;

        /**
         * @param timeout 哨兵节点则会null
         */
        public TimerTaskEntry(@Nullable LevelWheelTimeout timeout, long expirationMs) {
            this.timeout = timeout;
            this.expirationMs = expirationMs;

            if (Objects.nonNull(timeout)) {
                //更新延迟任务绑定的entry
                timeout.setTimerTaskEntry(this);
            }
        }

        /**
         * @return 延迟任务是否被取消
         */
        public boolean isCancelled() {
            return timeout.getEntry() != this;
        }

        /**
         * 从所在的bucket list移除该entry
         */
        void remove() {
            TimerTaskList currentList = list;
            while (Objects.nonNull(currentList)) {
                //如果另外一remove entry在另外一个线程从该list移除entry, 下面操作可能会失败, 故这里重试.
                currentList.remove(this);
                currentList = list;
            }
        }

        @Override
        public int compare(TimerTaskEntry e1, TimerTaskEntry e2) {
            return Long.compare(e1.expirationMs, e2.expirationMs);
        }
    }

    /**
     * 把{@link TimerTask}封装成{@link Timeout}实现
     */
    private static class LevelWheelTimeout implements Timeout {
        /** 所在{@link Timer}实现 */
        private final Timer timer;
        /** 实际延迟任务 */
        private final TimerTask task;

        /** 绑定的bucket list entry, 如果为null, 则是被cancelled */
        @Nullable
        private TimerTaskEntry entry;

        public LevelWheelTimeout(Timer timer, TimerTask task) {
            this.timer = timer;
            this.task = task;
        }

        /**
         * 绑定entry
         */
        synchronized void setTimerTaskEntry(TimerTaskEntry entry) {
            if (Objects.nonNull(this.entry) && entry != this.entry) {
                //先remove
                this.entry.remove();
            }
            this.entry = entry;
        }

        /**
         * 返回绑定的entry
         */
        TimerTaskEntry getEntry() {
            return entry;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean isExpired() {
            if (Objects.isNull(entry)) {
                //已取消
                return false;
            } else {
                //可能在bucket list中
                TimerTaskList bucket = entry.list;
                if (Objects.isNull(bucket)) {
                    //不在bucket list中, 则是过期
                    return true;
                } else {
                    //在bucket list中
                    return entry.expirationMs >= bucket.timer.currentTime;
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return Objects.isNull(entry);
        }

        @Override
        public synchronized boolean cancel() {
            //cancel timer task, 则会将entry设置为null
            setTimerTaskEntry(null);
            return true;
        }

        @Override
        public String toString() {
            String stateDesc = "";
            if (Objects.isNull(entry)) {
                //已取消
                stateDesc = "state: cancelled";
            } else {
                //可能在bucket list中
                TimerTaskList bucket = entry.list;
                if (Objects.isNull(bucket)) {
                    //不在bucket list中, 则是过期
                    stateDesc = "state: expired";
                } else {
                    //在bucket list中
                    stateDesc = String.format("deadline: %d ms later", entry.expirationMs - bucket.timer.currentTime);
                }
            }
            return String.format("LevelWheelTimeout(%s, task: %s)", stateDesc, task);
        }
    }
}
