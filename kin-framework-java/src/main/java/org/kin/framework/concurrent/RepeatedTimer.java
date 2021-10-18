package org.kin.framework.concurrent;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 支持重复开启关闭的计时器
 * 支持每次计时动态调整计时时间
 *
 * @author huangjianqin
 * @date 2021/10/11
 */
public abstract class RepeatedTimer {
    protected static final Logger log = LoggerFactory.getLogger(RepeatedTimer.class);

    /** 计时器状态 > 调度中 */
    private static final byte RUNNING = 1;
    /** 计时器状态 > 停止 */
    private static final byte STOPPED = 2;
    /** 计时器状态 > 销毁 */
    private static final byte DESTROYED = 3;

    /**
     * 获取计时器状态描述
     */
    private static String getStateDesc(int state) {
        if (state == RUNNING) {
            return "running";
        } else if (state == STOPPED) {
            return "stopped";
        } else if (state == DESTROYED) {
            return "destroyed";
        } else {
            return "unknown";
        }
    }

    /** 计时器名字 */
    private final String name;
    /** 计时时间(ms) */
    private volatile long timeoutMs;
    /** 切换状态时加锁 */
    private final Lock lock = new ReentrantLock();
    /** 计时器 */
    private final Timer timer;
    /** 计时器任务 */
    private Timeout timeout;
    /** 状态 */
    private volatile byte state;
    /** 是否invoking */
    private volatile boolean invoking;

    protected RepeatedTimer(String name, long timeoutMs) {
        this(name, timeoutMs, new HashedWheelTimer(new SimpleThreadFactory(name, true), 1, TimeUnit.MILLISECONDS, 2048));
    }

    protected RepeatedTimer(String name, long timeoutMs, Timer timer) {
        super();
        this.name = name;
        this.timeoutMs = timeoutMs;
        toStopped();
        Preconditions.checkNotNull(timer);
        this.timer = timer;
    }

    /**
     * 计时结束时触发
     */
    private final void trigger() {
        invoking = true;
        try {
            onTrigger();
        } catch (Throwable t) {
            log.error("run timer failed.", t);
        }
        boolean invokeDestroyed = false;
        lock.lock();
        try {
            invoking = false;
            if (!isRunning()) {
                invokeDestroyed = isDestroyed();
            } else {
                timeout = null;
                schedule();
            }
        } finally {
            lock.unlock();
        }
        if (invokeDestroyed) {
            onDestroy();
        }
    }

    /**
     * 触发一次计时结束, 会取消之前的计时任务, 并且重新调度计时任务
     */
    public final void triggerOnceNow() {
        lock.lock();
        try {
            if (timeout != null && timeout.cancel()) {
                timeout = null;
                trigger();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * timer start
     */
    public final void start() {
        lock.lock();
        try {
            if (!isStopped()) {
                return;
            }
            toRunning();
            schedule();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 调度计时任务
     */
    private final void schedule() {
        if (timeout != null) {
            timeout.cancel();
        }
        TimerTask timerTask = timeout -> {
            try {
                RepeatedTimer.this.trigger();
            } catch (Throwable t) {
                log.error("run timer task failed, taskName={}.", RepeatedTimer.this.name, t);
            }
        };
        timeout = timer.newTimeout(timerTask, adjustTimeout(timeoutMs), TimeUnit.MILLISECONDS);
    }

    /**
     * 重置计时时间
     */
    public final void reset(long timeoutMs) {
        lock.lock();
        this.timeoutMs = timeoutMs;
        try {
            if (isDestroyed()) {
                return;
            }

            if (isRunning()) {
                schedule();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 以当前计时时间重置
     */
    public final void reset() {
        reset(timeoutMs);
    }

    /**
     * timer destroy
     */
    public final void destroy() {
        boolean invokeDestroyed = false;
        lock.lock();
        try {
            if (isDestroyed()) {
                return;
            }

            if (isRunning()) {
                invokeDestroyed = true;
            }
            toDestroyed();
            if (timeout != null) {
                if (!timeout.cancel()) {
                    //取消计时任务失败
                    invokeDestroyed = true;
                }
                timeout = null;
            }
        } finally {
            lock.unlock();
            timer.stop();
            if (invokeDestroyed) {
                //等待计时任务完成时才触发destroy逻辑
                onDestroy();
            }
        }
    }

    /**
     * timer stop
     */
    public final void stop() {
        this.lock.lock();
        try {
            if (isStopped()) {
                return;
            }
            toStopped();
            if (timeout != null) {
                timeout.cancel();
                timeout = null;
            }
        } finally {
            this.lock.unlock();
        }
    }

    //子类可重写的方法

    /**
     * 计时结束触发的逻辑
     */
    protected abstract void onTrigger();

    /**
     * Adjust timeoutMs before every scheduling.
     *
     * @param timeoutMs timeout millis
     * @return timeout millis
     */
    protected long adjustTimeout(long timeoutMs) {
        //默认=设置计时时间, 支持重写
        return timeoutMs;
    }

    /**
     * timer destroy时触发
     */
    protected void onDestroy() {
        log.info("destroy timer: {}.", this);
    }

    //state change
    private boolean isRunning() {
        return state == RUNNING;
    }

    private boolean isStopped() {
        return state == STOPPED;
    }

    private boolean isDestroyed() {
        return state == DESTROYED;
    }

    private void toRunning() {
        state = RUNNING;
    }

    private void toStopped() {
        state = STOPPED;
    }

    private void toDestroyed() {
        state = DESTROYED;
    }

    //getter
    public long getTimeoutMs() {
        return this.timeoutMs;
    }

    @Override
    public String toString() {
        return "RepeatedTimer{" +
                "name='" + name + '\'' +
                ", timeoutMs=" + timeoutMs +
                ", lock=" + lock +
                ", timeout=" + timeout +
                ", state=" + getStateDesc(state) +
                ", invoking=" + invoking +
                '}';
    }
}
