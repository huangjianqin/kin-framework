package org.kin.framework.event;

import org.kin.framework.concurrent.DefaultPartitionExecutor;
import org.kin.framework.concurrent.EfficientHashPartitioner;
import org.kin.framework.concurrent.HashedWheelTimer;
import org.kin.framework.concurrent.Timeout;
import org.kin.framework.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 事件分发器
 * 支持多线程事件处理
 * 同一事件类型, 有序处理
 *
 * @author huangjianqin
 * @date 2020/12/9
 */
public class DefaultOrderedEventDispatcher extends DefaultEventDispatcher implements ScheduledDispatcher, ScheduledOrderedEventDispatcher {
    /** 事件处理线程(分区处理) */
    protected final DefaultPartitionExecutor<Integer> executor;
    /** 事件合并上下文 */
    protected final ConcurrentHashMap<Class<?>, EventMergeContext> mergeContexts = new ConcurrentHashMap<>();
    /** 时间轮, 用于控制事件合并window */
    protected final HashedWheelTimer wheelTimer = new HashedWheelTimer();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultOrderedEventDispatcher(int parallelism) {
        executor = new DefaultPartitionExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE, "ParallelEventDispatcher");
    }

    /**
     * 如果需要合并事件则合并, 否则直接执行dispatch(EventContext)方法
     */
    private void dispatch0(EventContext eventContext) {
        Object event = eventContext.getEvent();
        Class<?> eventClass = event.getClass();
        EventMerge eventMerge = eventClass.getAnnotation(EventMerge.class);
        if (Objects.nonNull(eventMerge)) {
            EventMergeContext eventMergeContext = CollectionUtils.putIfAbsent(mergeContexts, eventClass, new EventMergeContext(eventClass, eventMerge));
            eventMergeContext.mergeEvent(eventContext);
        } else {
            execDispatch(eventContext);
        }
    }

    private void dispatch0(int partitionId, Object event) {
        dispatch0(new EventContext(partitionId, event));
    }

    @Override
    public final void dispatch(Object event) {
        dispatch(event.getClass().hashCode(), event);
    }

    @Override
    public final Future<?> schedule(Object event, long delay, TimeUnit unit) {
        return schedule(event.getClass().hashCode(), event, delay, unit);
    }

    @Override
    public final Future<?> scheduleAtFixRate(Object event, long initialDelay, long period, TimeUnit unit) {
        return scheduleAtFixRate(event.getClass().hashCode(), event, initialDelay, period, unit);
    }

    @Override
    public final Future<?> scheduleWithFixedDelay(Object event, long initialDelay, long delay, TimeUnit unit) {
        return scheduleWithFixedDelay(event.getClass().hashCode(), event, initialDelay, delay, unit);
    }

    @Override
    public final void dispatch(int partitionId, Object event) {
        executor.execute(partitionId, () -> dispatch0(partitionId, event));
    }

    @Override
    public final Future<?> schedule(int partitionId, Object event, long delay, TimeUnit unit) {
        return executor.schedule(partitionId, () -> dispatch0(partitionId, event), delay, unit);
    }

    @Override
    public final Future<?> scheduleAtFixRate(int partitionId, Object event, long initialDelay, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(partitionId, () -> dispatch0(partitionId, event), initialDelay, period, unit);
    }

    @Override
    public final Future<?> scheduleWithFixedDelay(int partitionId, Object event, long initialDelay, long delay, TimeUnit unit) {
        return executor.scheduleWithFixedDelay(partitionId, () -> dispatch0(partitionId, event), initialDelay, delay, unit);
    }

    @Override
    public final void dispatch(Runnable runnable) {
        executor.execute(runnable.getClass().hashCode(), runnable);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        wheelTimer.stop();
        mergeContexts.clear();
        super.shutdown();
    }

    //---------------------------------------------------------------------------------------------------------------------

    /**
     * 事件合并上下文
     */
    private class EventMergeContext {
        /** 缓存窗口时间的事件 */
        private final List<EventContext> eventContexts = new ArrayList<>();
        /** 事件合并参数 */
        private final EventMerge eventMerge;
        /** 事件类型 */
        private final Class<?> eventClass;
        /** 窗口标识 */
        private Timeout timeout;

        EventMergeContext(Class<?> eventClass, EventMerge eventMerge) {
            this.eventClass = eventClass;
            this.eventMerge = eventMerge;
        }

        /**
         * 合并事件
         */
        void mergeEvent(EventContext eventContext) {
            MergeType type = eventMerge.type();
            if (MergeType.WINDOW.equals(type)) {
                mergeWindowEvent(eventContext);
            } else if (MergeType.DEBOUNCE.equals(type)) {
                mergeDebounceEvent(eventContext);
            } else {
                throw new UnsupportedOperationException(String.format("unsupport merge type '%s'", type));
            }
        }

        /**
         * 分发事件合并集合
         */
        private void triggerMergedEvents() {
            mergeContexts.remove(eventClass);

            //根据partitionId区分不同的事件集合
            Map<Integer, List<EventContext>> partitionId2MergedEvents =
                    eventContexts.stream().collect(Collectors.groupingBy(EventContext::getPartitionId));
            for (Map.Entry<Integer, List<EventContext>> entry : partitionId2MergedEvents.entrySet()) {
                executor.execute(entry.getKey(),
                        () -> DefaultOrderedEventDispatcher.super.execDispatch0(
                                eventClass,
                                entry.getValue().stream().map(EventContext::getEvent).collect(Collectors.toList())
                        )
                );
            }
        }

        /**
         * 根据窗口规则, 合并事件
         */
        private void mergeWindowEvent(EventContext eventContext) {
            //启动window
            if (Objects.isNull(timeout)) {
                timeout = wheelTimer.newTimeout(t -> triggerMergedEvents(), eventMerge.window(), eventMerge.unit());
            }

            eventContexts.add(eventContext);
        }

        /**
         * 根据抖动规则, 合并事件
         */
        private void mergeDebounceEvent(EventContext eventContext) {
            //启动window
            if (Objects.nonNull(timeout)) {
                //重置
                timeout.cancel();
            }
            timeout = wheelTimer.newTimeout(t -> triggerMergedEvents(), eventMerge.window(), eventMerge.unit());

            eventContexts.add(eventContext);
        }
    }
}
