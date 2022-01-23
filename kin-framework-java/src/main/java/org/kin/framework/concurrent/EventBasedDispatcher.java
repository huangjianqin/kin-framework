package org.kin.framework.concurrent;

import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 底层消息处理实现是基于事件处理
 * 消息有序处理, 但不保证在同一线程下执行, 不要使用ThreadLocal
 * 尽量不要blocking
 *
 * @author huangjianqin
 * @date 2020-04-15
 * <p>
 */
public final class EventBasedDispatcher<KEY, MSG> extends AbstractDispatcher<KEY, MSG> {
    private static final Logger log = LoggerFactory.getLogger(EventBasedDispatcher.class);
    /** 毒药, 终止message loop */
    private final ReceiverData<MSG> POISON_PILL = new ReceiverData<>(null, false);

    /** 并发数 */
    private final int parallelism;
    /** Receiver数据 */
    private final Map<KEY, ReceiverData<MSG>> receiverDatas = new ConcurrentHashMap<>();
    /** 等待数据处理的receivers */
    //TODO 考虑增加标志位, 在线程安全模式下, 如果Receiver消息正在被处理, 则不需要入队, 减少队列长度, 但这样子就会存在'比较忙'的Receiver长期占用, 其他Receiver消息得不到处理的问题
    private final LinkedBlockingQueue<ReceiverData<MSG>> pendingDatas = new LinkedBlockingQueue<>();
    /** 是否已启动message loop */
    private volatile boolean isMessageLoopRun;

    public EventBasedDispatcher(int parallelism) {
        super(ExecutionContext.forkjoin(
                parallelism, "eventBasedDispatcher",
                SysUtils.CPU_NUM / 2 + 1));
        this.parallelism = parallelism;
    }

    @Override
    public void register(KEY key, Receiver<MSG> receiver, boolean enableConcurrent) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(receiver)) {
            throw new IllegalArgumentException("arg 'key' or 'receiver' is null");
        }

        if (Objects.nonNull(receiverDatas.putIfAbsent(key, new ReceiverData<>(receiver, enableConcurrent)))) {
            throw new IllegalArgumentException(String.format("%s has registered", key));
        }

        ReceiverData<MSG> data = receiverDatas.get(key);
        pendingDatas.offer(data);

        //lazy init
        if (!isMessageLoopRun) {
            synchronized (this) {
                if (!isMessageLoopRun) {
                    for (int i = 0; i < parallelism; i++) {
                        executionContext.execute(new MessageLoop());
                    }
                    isMessageLoopRun = true;
                }
            }
        }
    }

    @Override
    public void unregister(KEY key) {
        if (Objects.isNull(key)) {
            throw new IllegalArgumentException("arg 'key' is null");
        }

        ReceiverData<MSG> data = receiverDatas.remove(key);
        if (Objects.nonNull(data)) {
            data.inBox.close();
            pendingDatas.offer(data);
        }
    }

    @Override
    public boolean isRegistered(KEY key) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        return receiverDatas.containsKey(key);
    }

    @Override
    public void postMessage(KEY key, MSG message) {
        if (isStopped()) {
            throw new IllegalStateException("dispatcher is closed");
        }

        if (Objects.isNull(key) || Objects.isNull(message)) {
            throw new IllegalArgumentException("arg 'key' or 'message' is null");
        }

        ReceiverData<MSG> data = receiverDatas.get(key);
        if (Objects.nonNull(data)) {
            data.inBox.post(new InBox.OnMessageSignal<>(message));
            pendingDatas.offer(data);
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
        for (KEY key : receiverDatas.keySet()) {
            postMessage(key, message);
        }
    }

    @Override
    protected void doClose() {
        receiverDatas.keySet().forEach(this::unregister);
        pendingDatas.offer(POISON_PILL);

        //help gc
        receiverDatas.clear();
        pendingDatas.clear();
    }

    //------------------------------------------------------------------------------------------------------------------------
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
                    data.inBox.process();
                }
            } catch (InterruptedException e) {
                //do nothing
            } catch (Exception e) {
                log.error("", e);
                try {
                    //re-run
                    executionContext.execute(new MessageLoop());
                } finally {
                    throw e;
                }
            }
        }
    }

    private static class ReceiverData<MSG> {
        private final InBox<MSG> inBox;

        private ReceiverData(Receiver<MSG> receiver, boolean enableConcurrent) {
            inBox = new InBox<>(receiver, enableConcurrent);
        }
    }
}
