package org.kin.framework.concurrent;

import org.kin.framework.utils.Maths;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * {@link EventExecutor}池
 * 基于{@link FastThreadLocalThreadFactory}
 * 默认使用{@link FastThreadLocalThread}
 * <p>
 * 抽象父类
 * 子类实现时, 需提供注册绑定EventExecutor的接口, 用于给外部组件绑定固定的EventExecutor, 类似于netty的EventLoop与channel的绑定
 * <p>
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 *
 * @author huangjianqin
 * @date 2020/11/23
 */
public abstract class AbstractEventExecutorPool implements EventExecutorGroup {
    //状态枚举
    private static final byte ST_STARTED = 1;
    private static final byte ST_SHUTDOWN = 2;
    private static final byte ST_TERMINATED = 3;

    /** 原子更新状态值 */
    private static final AtomicIntegerFieldUpdater<AbstractEventExecutorPool> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(AbstractEventExecutorPool.class, "state");

    /** 状态值 */
    private volatile int state = ST_STARTED;

    /** {@link EventExecutor}选择逻辑 */
    private final EventExecutorChooser chooser;
    /** {@link EventExecutor}实例绑定的线程池 */
    private final ExecutorService executor;
    /** {@link EventExecutor}实例 */
    private final EventExecutor[] eventExecutors;

    public AbstractEventExecutorPool(int coreSize) {
        this(coreSize, getDefaultChooser(coreSize), "eventExecutorPool");
    }

    public AbstractEventExecutorPool(int coreSize, EventExecutorChooser chooser) {
        this(coreSize, chooser, "eventExecutorPool");
    }

    public AbstractEventExecutorPool(int coreSize, EventExecutorChooser chooser, String workerNamePrefix) {
        this(coreSize, chooser, ExecutionContext.fix(coreSize, new FastThreadLocalThreadFactory(workerNamePrefix)));
    }

    public AbstractEventExecutorPool(int coreSize, EventExecutorChooser chooser, ThreadFactory threadFactory) {
        this(coreSize, chooser, ExecutionContext.fix(coreSize, threadFactory));
    }

    public AbstractEventExecutorPool(int coreSize, String workerNamePrefix) {
        this(coreSize, getDefaultChooser(coreSize), ExecutionContext.fix(coreSize, new FastThreadLocalThreadFactory(workerNamePrefix)));
    }

    public AbstractEventExecutorPool(int coreSize, ThreadFactory threadFactory) {
        this(coreSize, getDefaultChooser(coreSize), ExecutionContext.fix(coreSize, threadFactory));
    }

    public AbstractEventExecutorPool(int coreSize, ExecutorService executor) {
        this(coreSize, getDefaultChooser(coreSize), executor);
    }

    public AbstractEventExecutorPool(int coreSize, EventExecutorChooser chooser, ExecutorService executor) {
        this.chooser = chooser;
        this.executor = executor;
        this.eventExecutors = new EventExecutor[coreSize];
        for (int i = 0; i < coreSize; i++) {
            eventExecutors[i] = newEventExecutor(this.executor);
        }
    }

    /**
     * 获取默认的{@link EventExecutorChooser}实现
     */
    private static EventExecutorChooser getDefaultChooser(int coreSize) {
        if (Maths.isPower2(coreSize)) {
            return new PowerOfTwoEventExecutorChooser();
        } else {
            return new GenericEventExecutorChooser();
        }
    }

    /**
     * 自定义构造{@link EventExecutor}逻辑
     */
    protected abstract EventExecutor newEventExecutor(ExecutorService executor);

    /**
     * 从池中选择一个EventExecutor
     */
    protected final EventExecutor choose() {
        return chooser.choose(eventExecutors);
    }

    /**
     * 自定义shutdown逻辑
     */
    protected void customShutdown() {
        //default empty
    }

    /**
     * shutdown
     */
    @Override
    public final void shutdown() {
        //shutdown
        for (EventExecutor executor : eventExecutors) {
            executor.shutdown();
        }
        executor.shutdown();

        for (; ; ) {
            //等待所有Executor Shutdown
            boolean allShutdown = true;
            for (EventExecutor executor : eventExecutors) {
                if (!executor.isShutdown()) {
                    allShutdown = false;
                    break;
                }
            }

            if (allShutdown) {
                for (; ; ) {
                    int oldState = state;
                    if (oldState >= ST_SHUTDOWN || STATE_UPDATER.compareAndSet(
                            this, oldState, ST_SHUTDOWN)) {
                        break;
                    }
                }
                break;
            }
        }

        for (; ; ) {
            //等待所有Executor Terminated
            boolean allTerminated = true;
            for (EventExecutor executor : eventExecutors) {
                if (!executor.isTerminated()) {
                    allTerminated = false;
                    break;
                }
            }

            if (allTerminated) {
                break;
            }
        }
        STATE_UPDATER.set(this, ST_TERMINATED);

        customShutdown();
    }

    /**
     * @return 是否Terminated
     */
    @Override
    public final boolean isTerminated() {
        return state >= ST_TERMINATED;
    }

    @Override
    public EventExecutor next() {
        return choose();
    }

    @Override
    public final boolean isShutdown() {
        return state >= ST_SHUTDOWN;
    }

    @Override
    public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop:
        for (EventExecutor l : eventExecutors) {
            for (; ; ) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }

    //getter

    /**
     * @return 总线程数
     */
    public final int getCoreSize() {
        return eventExecutors.length;
    }
}
