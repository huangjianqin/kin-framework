package org.kin.framework.event;

import org.kin.framework.concurrent.EfficientHashPartitioner;
import org.kin.framework.concurrent.PartitionTaskExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
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
        super(isEnhance);
        executor = new PartitionTaskExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE, "EventDispatcher$event-handler-");

        scheduledExecutors = new ScheduledThreadPoolExecutor(SysUtils.getSuitableThreadNum() / 2 + 1,
                new SimpleThreadFactory("EventDispatcher$schedule-event-"));
    }


    @Override
    public final void dispatch(Object event) {
        dispatch(event, EventCallback.EMPTY);
    }

    @Override
    public final void dispatch(Object event, EventCallback callback) {
        int partitionId = event.hashCode();
        executor.execute(partitionId, () -> dispatch(new EventContext(partitionId, event, callback)));
    }

    @Override
    public final Future<?> scheduleDispatch(Object event, TimeUnit unit, long delay) {
        return scheduledExecutors.schedule(() -> dispatch(event), delay, unit);
    }

    @Override
    public final Future<?> scheduleDispatchAtFixRate(Object event, TimeUnit unit, long initialDelay, long period) {
        return scheduledExecutors.scheduleAtFixedRate(() -> dispatch(event), initialDelay, period, unit);
    }

    @Override
    public final void dispatch(Runnable runnable) {
        executor.execute(runnable.hashCode(), runnable);
    }

    @Override
    public final void shutdown() {
        executor.shutdown();
        scheduledExecutors.shutdown();

        super.shutdown();
    }
}
