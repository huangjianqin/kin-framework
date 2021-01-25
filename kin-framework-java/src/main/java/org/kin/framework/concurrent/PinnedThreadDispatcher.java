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
public final class PinnedThreadDispatcher<KEY, MSG> extends AbstractDispatcher<KEY, MSG> {
    /** Receiver数据 */
    private Map<KEY, PinnedThreadReceiver<MSG>> receivers = new ConcurrentHashMap<>();

    public PinnedThreadDispatcher(int parallelism) {
        this(parallelism, "pinnedDispatcher");
    }

    public PinnedThreadDispatcher(int parallelism, String workerNamePrefix) {
        super(ExecutionContext.elastic(
                Math.min(parallelism, SysUtils.CPU_NUM * 10), SysUtils.CPU_NUM * 10, workerNamePrefix,
                SysUtils.CPU_NUM / 2 + 1, workerNamePrefix.concat("-schedule")));
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
            if (Objects.nonNull(receivers.putIfAbsent(key, new PinnedThreadReceiver<>(receiver)))) {
                throw new IllegalArgumentException(String.format("%s has registried", key));
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
            PinnedThreadReceiver<MSG> pinnedThreadReceiver = receivers.remove(key);
            if (Objects.nonNull(pinnedThreadReceiver)) {
                pinnedThreadReceiver.onStop();
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

        PinnedThreadReceiver<MSG> pinnedThreadReceiver = receivers.get(key);
        if (Objects.nonNull(pinnedThreadReceiver)) {
            pinnedThreadReceiver.receive(message);
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
    private class PinnedThreadReceiver<M> extends Receiver<M> {
        /** executor */
        private PinnedThreadExecutor executor;
        /** Receiver实例 */
        private Receiver<M> proxy;

        private PinnedThreadReceiver(Receiver<M> receiver) {
            this.executor = new PinnedThreadExecutor(PinnedThreadDispatcher.super.executionContext);
            this.proxy = receiver;
        }

        @Override
        public void receive(M mail) {
            executor.handle(pal -> proxy.receive(mail));
        }

        @Override
        protected void onStart() {
            executor.handle(pal -> proxy.onStart());
        }

        @Override
        protected void onStop() {
            executor.handle(pal -> proxy.onStop());
        }
    }
}
