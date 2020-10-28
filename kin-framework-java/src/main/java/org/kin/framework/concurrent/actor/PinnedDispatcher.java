package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020-04-26
 * <p>
 * 底层消息处理实现是每个Receiver绑定一条线程, 该线程由一个线程池管理(该线程池可以固定线程数, 也可以无限线程数)
 * 无上限分区
 * 可以blocking, 但要控制好parallelism, 保证有足够的线程数
 */
public class PinnedDispatcher<KEY, MSG> extends AbstractDispatcher<KEY, MSG> {
    private static final Logger log = LoggerFactory.getLogger(PinnedDispatcher.class);
    /** Receiver数据 */
    private Map<KEY, PinnedThreadSafeReceiver<MSG>> typeSafeReceivers = new ConcurrentHashMap<>();

    public PinnedDispatcher(int parallelism) {
        this(parallelism, "pinnedDispatcher");
    }

    public PinnedDispatcher(int parallelism, String workerNamePrefix) {
        super(ExecutionContext.cache(
                parallelism, workerNamePrefix,
                parallelism / 2 + 1, workerNamePrefix.concat("-schedule")));
    }

    @Override
    protected void doClose() {
        typeSafeReceivers.keySet().forEach(this::unregister);
    }

    @Override
    protected void doRegister(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        if (enableConcurrent) {
            throw new IllegalArgumentException("pinnedDispatcher doesn't support concurrent");
        }

        if (Objects.nonNull(typeSafeReceivers.putIfAbsent(key, new PinnedThreadSafeReceiver<>(receiver)))) {
            throw new IllegalArgumentException(String.format("%s has registried", key));
        }

        typeSafeReceivers.get(key).onStart();
    }

    @Override
    protected void doUnRegister(KEY key) {
        PinnedThreadSafeReceiver<MSG> pinnedThreadSafeReceiver = typeSafeReceivers.remove(key);
        if (Objects.nonNull(pinnedThreadSafeReceiver)) {
            pinnedThreadSafeReceiver.onStart();
        }
    }

    @Override
    protected void doPostMessage(KEY key, MSG message) {
        PinnedThreadSafeReceiver<MSG> pinnedThreadSafeReceiver = typeSafeReceivers.get(key);
        if (Objects.nonNull(pinnedThreadSafeReceiver)) {
            pinnedThreadSafeReceiver.receive(message);
        }
    }

    @Override
    protected void doPost2All(MSG message) {
        for (KEY key : typeSafeReceivers.keySet()) {
            doPostMessage(key, message);
        }
    }

    @Override
    public boolean isRegistered(KEY key) {
        return typeSafeReceivers.containsKey(key);
    }

    private class PinnedThreadSafeReceiver<M> extends Receiver<M> {
        private PinnedThreadSafeHandler threadSafeHandler;
        private Receiver<M> proxy;

        private PinnedThreadSafeReceiver(Receiver<M> receiver) {
            this.threadSafeHandler = new PinnedThreadSafeHandler(PinnedDispatcher.super.executionContext);
            this.proxy = receiver;
        }

        @Override
        public void receive(M mail) {
            threadSafeHandler.handle(pal -> proxy.receive(mail));
        }

        @Override
        protected void onStart() {
            threadSafeHandler.handle(pal -> proxy.onStart());
        }

        @Override
        protected void onStop() {
            threadSafeHandler.handle(pal -> proxy.onStop());
        }
    }
}
