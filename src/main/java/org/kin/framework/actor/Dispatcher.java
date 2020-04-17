package org.kin.framework.actor;

import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author huangjianqin
 * @date 2020-04-15
 */
public class Dispatcher<KEY, MSG> implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    private final ReceiverData<MSG> POISON_PILL = new ReceiverData<>(null, false);

    private ExecutionContext executionContext;
    private int parallelism;
    private Map<KEY, ReceiverData<MSG>> receiverDatas = new ConcurrentHashMap<>();
    private LinkedBlockingQueue<ReceiverData<MSG>> pendingDatas = new LinkedBlockingQueue<>();
    private boolean stopped;

    public Dispatcher(int parallelism) {
        this.parallelism = parallelism;
        this.executionContext =
                ExecutionContext.forkjoin(
                        parallelism, "dispatcher",
                        SysUtils.getSuitableThreadNum() / 2 + 1, "dispatcher-schedule");
    }

    public void init() {
        for (int i = 0; i < parallelism; i++) {
            executionContext.execute(new MessageLoop());
        }
    }

    public void register(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        synchronized (this) {
            if (stopped) {
                throw new IllegalStateException("dispatcher has been stopped");
            }

            if (Objects.nonNull(receiverDatas.putIfAbsent(key, new ReceiverData<>(receiver, enableConcurrent)))) {
                throw new IllegalArgumentException(String.format("%s has registried", key));
            }

            ReceiverData<MSG> data = receiverDatas.get(key);
            pendingDatas.offer(data);
        }
    }

    public void register(KEY key, Receiver<MSG> receiver) {
        register(key, receiver, false);
    }

    public void unRegister(KEY key) {
        ReceiverData<MSG> data = receiverDatas.get(key);
        if (Objects.nonNull(data)) {
            data.inBox.close();
            pendingDatas.offer(data);
        }
    }

    public void postMessage(KEY key, MSG message) {
        synchronized (this) {
            if (stopped) {
                return;
            }

            ReceiverData<MSG> data = receiverDatas.get(key);
            if (Objects.nonNull(data)) {
                data.inBox.post(new OnMessageSignal<>(message));
                pendingDatas.offer(data);
            }
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (stopped) {
                return;
            }

            stopped = true;
        }

        receiverDatas.keySet().forEach(this::unRegister);
        pendingDatas.offer(POISON_PILL);
        executionContext.shutdown();
    }

    private class MessageLoop implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    ReceiverData<MSG> data = pendingDatas.take();
                    if (data == POISON_PILL) {
                        pendingDatas.offer(POISON_PILL);
                        return;
                    }
                    data.inBox.process(Dispatcher.this);
                }
            } catch (InterruptedException e) {

            } catch (Exception e) {
                ExceptionUtils.log(e);
                try {
                    //re-run
                    executionContext.execute(new MessageLoop());
                } finally {
                    throw e;
                }
            }
        }
    }

    private static class ReceiverData<M> {
        private InBox<M> inBox;

        public ReceiverData(Receiver<M> receiver, boolean enableConcurrent) {
            inBox = new InBox<>(receiver, enableConcurrent);
        }
    }
}
