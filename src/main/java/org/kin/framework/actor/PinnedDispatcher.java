package org.kin.framework.actor;

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
    private static final Logger log = LoggerFactory.getLogger(EventBaseDispatcher.class);
    private Map<KEY, ActorLikeReceiver<MSG>> actorLikeReceivers = new ConcurrentHashMap<>();

    public PinnedDispatcher(int parallelism) {
        super(ExecutionContext.fix(
                parallelism, "pinnedThread-dispatcher",
                SysUtils.getSuitableThreadNum() / 2 + 1, "pinnedThread-dispatcher-schedule"));
    }


    @Override
    public void doInit() {

    }

    @Override
    public void doClose() {
        actorLikeReceivers.keySet().forEach(this::unRegister);
    }

    @Override
    public void doRegister(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        if (enableConcurrent) {
            throw new IllegalArgumentException("pinnedThread-dispatcher doesn't support concurrent");
        }

        if (Objects.nonNull(actorLikeReceivers.putIfAbsent(key, new ActorLikeReceiver<>(receiver)))) {
            throw new IllegalArgumentException(String.format("%s has registried", key));
        }

        actorLikeReceivers.get(key).onStart();
    }

    @Override
    public void doUnRegister(KEY key) {
        ActorLikeReceiver<MSG> actorLikeReceiver = actorLikeReceivers.remove(key);
        if (Objects.nonNull(actorLikeReceiver)) {
            actorLikeReceiver.onStart();
        }
    }

    @Override
    public void doPostMessage(KEY key, MSG message) {
        ActorLikeReceiver<MSG> actorLikeReceiver = actorLikeReceivers.get(key);
        if (Objects.nonNull(actorLikeReceiver)) {
            actorLikeReceiver.receive(message);
        }
    }

    private class PinnedActorLike extends ActorLike<PinnedActorLike> {
        private PinnedActorLike() {
            super(PinnedDispatcher.super.executionContext);
        }
    }

    private class ActorLikeReceiver<M> extends Receiver<M> {
        private PinnedActorLike pinnedActorLike;
        private Receiver<M> proxy;

        private ActorLikeReceiver(Receiver<M> receiver) {
            this.pinnedActorLike = new PinnedActorLike();
            this.proxy = receiver;
        }

        @Override
        public void receive(M mail) {
            pinnedActorLike.tell(pal -> proxy.receive(mail));
        }

        @Override
        protected void onStart() {
            pinnedActorLike.tell(pal -> proxy.onStart());
        }

        @Override
        protected void onStop() {
            pinnedActorLike.tell(pal -> proxy.onStop());
        }
    }
}
