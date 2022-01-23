package org.kin.framework.concurrent;

import org.kin.framework.utils.SysUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 底层消息处理实现是每个Receiver绑定一条线程, 该线程由一个线程池管理(该线程池可以固定线程数, 也可以无限线程数)
 * 无上限分区
 * 可以blocking, 但要控制好parallelism, 保证有足够的线程数
 *
 * @author huangjianqin
 * @date 2020-04-26
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class OrderedEventDispatcher<KEY, MSG> extends AbstractDispatcher<KEY, MSG> {
    /** OrderedEventLoopGroup */
    private final CachedOrderedEventLoopGroup group;
    /** Receiver数据 */
    private final Map<KEY, InternalReceiver<MSG>> receivers = new ConcurrentHashMap<>();

    public OrderedEventDispatcher(int parallelism) {
        this(parallelism, "orderedEventDispatcher");
    }

    public OrderedEventDispatcher(int parallelism, String workerNamePrefix) {
        super(ExecutionContext.elastic(
                Math.min(parallelism, SysUtils.CPU_NUM * 10), SysUtils.CPU_NUM * 10, workerNamePrefix,
                SysUtils.CPU_NUM / 2 + 1));
        group = new CachedOrderedEventLoopGroup(executionContext, OrderedEventLoop::new);
    }

    @Override
    public void register(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(receiver)) {
            throw new IllegalArgumentException("arg 'key' or 'receiver' is null");
        }

        if (enableConcurrent) {
            throw new IllegalArgumentException("pinnedDispatcher doesn't support concurrent");
        }

        //保证receiver 先进行start, 后stop
        synchronized (this) {
            if (Objects.nonNull(receivers.putIfAbsent(key, new InternalReceiver<>(receiver)))) {
                throw new IllegalArgumentException(String.format("%s has registered", key));
            }

            receivers.get(key).onStart();
        }
    }

    @Override
    public void unregister(KEY key) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key)) {
            throw new IllegalArgumentException("arg 'key' is null");
        }

        //保证receiver 先进行start, 后stop
        synchronized (this) {
            InternalReceiver<MSG> receiver = receivers.remove(key);
            if (Objects.nonNull(receiver)) {
                receiver.onStop();
            }
        }
    }

    @Override
    public boolean isRegistered(KEY key) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key)) {
            throw new IllegalArgumentException("arg 'key' is null");
        }
        return receivers.containsKey(key);
    }

    @Override
    public void postMessage(KEY key, MSG message) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(message)) {
            throw new IllegalArgumentException("arg 'key' or 'message' is null");
        }

        InternalReceiver<MSG> internalReceiver = receivers.get(key);
        if (Objects.nonNull(internalReceiver)) {
            internalReceiver.receive(message);
        }
    }

    @Override
    public void post2All(MSG message) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(message)) {
            throw new IllegalArgumentException("arg 'message' is null");
        }
        for (KEY key : receivers.keySet()) {
            postMessage(key, message);
        }
    }

    @Override
    protected void doClose() {
        receivers.keySet().forEach(this::unregister);

        //help gc
        receivers.clear();
    }

    //-----------------------------------------------------------------------------------------------------

    /**
     * 线程安全receiver
     */
    private class InternalReceiver<M> extends Receiver<M> {
        /** executor */
        private EventLoop loop;
        /** Receiver实例 */
        private Receiver<M> proxy;

        private InternalReceiver(Receiver<M> receiver) {
            this.loop = group.next();
            this.proxy = receiver;
        }

        @Override
        public void receive(M mail) {
            loop.receive(pal -> proxy.receive(mail));
        }

        @Override
        protected void onStart() {
            loop.receive(pal -> proxy.onStart());
        }

        @Override
        protected void onStop() {
            loop.receive(pal -> {
                proxy.onStop();
                loop.shutdown();
            });
        }
    }
}
