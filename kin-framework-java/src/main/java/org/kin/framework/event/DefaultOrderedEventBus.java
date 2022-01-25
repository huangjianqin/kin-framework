package org.kin.framework.event;

import org.kin.framework.concurrent.DefaultPartitionExecutor;
import org.kin.framework.concurrent.EfficientHashPartitioner;
import org.kin.framework.concurrent.ExecutionContext;

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
public class DefaultOrderedEventBus extends DefaultEventBus implements ScheduledEventBus, ScheduledOrderedEventBus {
    /** 底层线程池管理 */
    protected final ExecutionContext ec;
    /** 事件处理线程(分区处理) */
    protected final DefaultPartitionExecutor<Integer> executor;
    /** 事件合并上下文 */
    protected final ConcurrentHashMap<Class<?>, EventMergeContext> mergeContexts = new ConcurrentHashMap<>();

    public DefaultOrderedEventBus(int parallelism) {
        this(parallelism, true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultOrderedEventBus(int parallelism, boolean isEnhance) {
        super(isEnhance);
        ec = ExecutionContext.fix(parallelism, "orderedEventBus", 3);
        executor = new DefaultPartitionExecutor<>(parallelism, EfficientHashPartitioner.INSTANCE, ec);
    }

    /**
     * 如果需要合并事件则合并, 否则直接执行doPost(EventContext)方法
     */
    private void post0(EventContext eventContext) {
        Object event = eventContext.getEvent();
        Class<?> eventClass = event.getClass();
        EventMerge eventMerge = eventClass.getAnnotation(EventMerge.class);
        if (Objects.nonNull(eventMerge)) {
            EventMergeContext eventMergeContext = mergeContexts.computeIfAbsent(eventClass, k -> new EventMergeContext(eventClass, eventMerge));
            eventMergeContext.mergeEvent(eventContext);
        } else {
            doPost(eventContext);
        }
    }

    private void post0(int partitionId, Object event) {
        post0(new EventContext(partitionId, event));
    }

    /**
     * @return 分区id, 默认按类分区
     */
    private int getPartitionId(Object obj) {
        return obj.getClass().hashCode();
    }

    @Override
    public final void post(Object event) {
        post(getPartitionId(event), event);
    }

    @Override
    public final Future<?> schedule(Object event, long delay, TimeUnit unit) {
        return schedule(getPartitionId(event), event, delay, unit);
    }

    @Override
    public final Future<?> scheduleAtFixRate(Object event, long initialDelay, long period, TimeUnit unit) {
        return scheduleAtFixRate(getPartitionId(event), event, initialDelay, period, unit);
    }

    @Override
    public final Future<?> scheduleWithFixedDelay(Object event, long initialDelay, long delay, TimeUnit unit) {
        return scheduleWithFixedDelay(getPartitionId(event), event, initialDelay, delay, unit);
    }

    @Override
    public final void post(int partitionId, Object event) {
        executor.execute(partitionId, () -> post0(partitionId, event));
    }

    @Override
    public final Future<?> schedule(int partitionId, Object event, long delay, TimeUnit unit) {
        return executor.schedule(partitionId, () -> post0(partitionId, event), delay, unit);
    }

    @Override
    public final Future<?> scheduleAtFixRate(int partitionId, Object event, long initialDelay, long period, TimeUnit unit) {
        return executor.scheduleAtFixedRate(partitionId, () -> post0(partitionId, event), initialDelay, period, unit);
    }

    @Override
    public final Future<?> scheduleWithFixedDelay(int partitionId, Object event, long initialDelay, long delay, TimeUnit unit) {
        return executor.scheduleWithFixedDelay(partitionId, () -> post0(partitionId, event), initialDelay, delay, unit);
    }

    @Override
    public final void shutdown() {
        executor.shutdown();
        ec.shutdown();
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
        /** {@link Future} */
        private Future<?> future;

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
                throw new UnsupportedOperationException(String.format("doesn't support merge type '%s'", type));
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
                        () -> DefaultOrderedEventBus.super.doPost(
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
            if (Objects.isNull(future)) {
                future = ec.schedule(this::triggerMergedEvents, eventMerge.window(), eventMerge.unit());
            }

            eventContexts.add(eventContext);
        }

        /**
         * 根据抖动规则, 合并事件
         */
        private void mergeDebounceEvent(EventContext eventContext) {
            //启动window
            if (Objects.nonNull(future)) {
                //重置
                future.cancel(true);
            }
            future = ec.schedule(this::triggerMergedEvents, eventMerge.window(), eventMerge.unit());

            eventContexts.add(eventContext);
        }
    }
}
