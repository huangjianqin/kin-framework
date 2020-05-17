package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020-04-26
 * <p>
 * 可以blocking, 但要控制好parallelism, 保证有足够的线程数
 */
public class PinnedDispatcher<KEY, MSG> extends AbstractDispatcher<KEY, MSG> {
    private static final Logger log = LoggerFactory.getLogger(EventBasedDispatcher.class);
    /** Receiver数据 */
    private Map<KEY, PinnedThreadSafeReceiver<MSG>> actorLikeReceivers = new ConcurrentHashMap<>();

    public PinnedDispatcher(int parallelism) {
        super(ExecutionContext.fix(
                parallelism, "pinnedDispatcher",
                SysUtils.getSuitableThreadNum() / 2 + 1, "pinnedDispatcher-schedule"));
    }

    @Override
    public void doClose() {
        actorLikeReceivers.keySet().forEach(this::unRegister);
    }

    @Override
    public void doRegister(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        if (enableConcurrent) {
            throw new IllegalArgumentException("pinnedDispatcher doesn't support concurrent");
        }

        if (Objects.nonNull(actorLikeReceivers.putIfAbsent(key, new PinnedThreadSafeReceiver<>(receiver)))) {
            throw new IllegalArgumentException(String.format("%s has registried", key));
        }

        actorLikeReceivers.get(key).onStart();
    }

    @Override
    public void doUnRegister(KEY key) {
        PinnedThreadSafeReceiver<MSG> pinnedThreadSafeReceiver = actorLikeReceivers.remove(key);
        if (Objects.nonNull(pinnedThreadSafeReceiver)) {
            pinnedThreadSafeReceiver.onStart();
        }
    }

    @Override
    public void doPostMessage(KEY key, MSG message) {
        PinnedThreadSafeReceiver<MSG> pinnedThreadSafeReceiver = actorLikeReceivers.get(key);
        if (Objects.nonNull(pinnedThreadSafeReceiver)) {
            pinnedThreadSafeReceiver.receive(message);
        }
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
