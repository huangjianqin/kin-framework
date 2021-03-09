package org.kin.framework.event;

import org.kin.framework.concurrent.DefaultPartitionExecutor;
import org.kin.framework.concurrent.EfficientHashPartitioner;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 事件分发器
 * 支持多线程事件处理
 *
 * @author huangjianqin
 * @date 2020/12/9
 */
public class ParallelEventDispatcher extends BasicEventDispatcher implements ScheduledDispatcher {
    /** 事件处理线程(分区处理) */
    protected final DefaultPartitionExecutor<Integer> executor;

    public ParallelEventDispatcher(int parallelism) {
        this(parallelism, false);
    }

    @SuppressWarnings("unchecked")
    public ParallelEventDispatcher(int parallelism, boolean isEnhance) {
        super(isEnhance);
        executor = new DefaultPartitionExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE, "EventDispatcher$event-handler");
    }

    /**
     * 直接执行dispatch(EventContext)方法
     */
    protected void dispatch0(int partitionId, Object event) {
        dispatch0(partitionId, event, EventCallback.EMPTY);
    }

    /**
     * 直接执行dispatch(EventContext)方法
     */
    protected void dispatch0(int partitionId, Object event, EventCallback callback) {
        dispatch(new EventContext(partitionId, event, callback));
    }

    @Override
    public final void dispatch(Object event) {
        dispatch(event, EventCallback.EMPTY);
    }

    @Override
    public final void dispatch(Object event, EventCallback callback) {
        int partitionId = event.hashCode();
        executor.execute(partitionId, () -> dispatch0(partitionId, event, callback));
    }

    @Override
    public final Future<?> scheduleDispatch(Object event, TimeUnit unit, long delay) {
        int partitionId = event.hashCode();
        return executor.schedule(partitionId, () -> dispatch0(partitionId, event), delay, unit);
    }

    @Override
    public final Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period) {
        int partitionId = event.hashCode();
        return executor.scheduleAtFixedRate(partitionId, () -> dispatch0(partitionId, event), initialDelay, period, unit);
    }

    @Override
    public Future<?> scheduleDispatchWithFixedDelay(Object event, TimeUnit unit, long initialDelay, long delay) {
        int partitionId = event.hashCode();
        return executor.scheduleWithFixedDelay(partitionId, () -> dispatch0(partitionId, event), initialDelay, delay, unit);
    }

    @Override
    public final void dispatch(Runnable runnable) {
        executor.execute(runnable.hashCode(), runnable);
    }

    @Override
    public final void shutdown() {
        executor.shutdown();

        super.shutdown();
    }
}
