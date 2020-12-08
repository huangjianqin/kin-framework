package org.kin.framework.event;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.partition.EfficientHashPartitioner;
import org.kin.framework.concurrent.partition.PartitionTaskExecutor;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 事件分发器
 * 支持多线程事件处理
 *
 * @author huangjianqin
 * @date 2020/12/9
 */
public class ParallelEventDispatcher extends EventDispatcher implements ScheduledDispatcher {
    /** 事件处理线程(分区处理) */
    protected final PartitionTaskExecutor<Integer> executor;
    /** 调度线程 */
    protected final ScheduledExecutorService scheduledExecutors;

    public ParallelEventDispatcher(int parallelism) {
        this(parallelism, false);
    }

    @SuppressWarnings("unchecked")
    public ParallelEventDispatcher(int parallelism, boolean isEnhance) {
        executor = new PartitionTaskExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE, "EventDispatcher$event-handler-");

        scheduledExecutors = new ScheduledThreadPoolExecutor(SysUtils.getSuitableThreadNum() / 2 + 1,
                new SimpleThreadFactory("EventDispatcher$schedule-event-"));
    }


    @Override
    public final void dispatch(Object event, Object... params) {
        dispatch(event, EventCallback.EMPTY, params);
    }

    @Override
    public final void dispatch(Object event, EventCallback callback, Object... params) {
        int partitionId = event.hashCode();
        executor.execute(partitionId, () -> dispatch(new EventContext(partitionId, event, params, callback)));
    }

    @Override
    public final Future<?> scheduleDispatch(Object event, TimeUnit unit, long delay, Object... params) {
        return scheduledExecutors.schedule(() -> dispatch(event, params), delay, unit);
    }

    @Override
    public final Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period, Object... params) {
        return scheduledExecutors.scheduleAtFixedRate(() -> dispatch(event, params), initialDelay, period, unit);
    }

    @Override
    public final void dispatch(Runnable runnable) {
        executor.execute(runnable.hashCode(), runnable);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        scheduledExecutors.shutdown();

        super.shutdown();
    }
}
