package org.kin.framework.concurrent;

import org.kin.framework.Closeable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;

/**
 * '邮箱'
 * 消息队列, 存储待处理的消息
 * @author huangjianqin
 * @date 2020-04-15
 */
class InBox<MSG> implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(InBox.class);

    /** 与'邮箱'绑定的Receiver */
    private final Receiver<MSG> receiver;
    /** 是否允许并发 */
    private final boolean enableConcurrent;
    /** 消息队列 */
    private LinkedList<InBoxMessage> mail = new LinkedList<>();
    /** 正在处理该'邮箱'消息的线程数 */
    private int activeThreads = 0;
    /** '邮箱'是否关了 */
    private boolean stopped;

    InBox(Receiver<MSG> receiver, boolean enableConcurrent) {
        this.receiver = receiver;
        this.enableConcurrent = enableConcurrent;
        mail.add(OnStartSignal.INSTANCE);
    }

    /**
     * 消息入队
     */
    public void post(InBoxMessage message) {
        synchronized (this) {
            if (stopped) {
                log.warn(String.format("Drop %s because %s is stopped", message, receiver));
                return;
            }
            mail.add(message);
        }
    }

    /**
     * 处理消息
     */
    @SuppressWarnings("unchecked")
    public void process() {
        InBoxMessage message;
        synchronized (this) {
            if (!enableConcurrent && activeThreads > 0) {
                return;
            }
            message = mail.poll();
            if (Objects.nonNull(message)) {
                activeThreads++;
            } else {
                return;
            }
        }

        while (true) {
            try {
                if (message instanceof OnStartSignal) {
                    receiver.onStart();
                } else if (message instanceof OnStopSignal) {
                    receiver.onStop();
                } else if (message instanceof OnMessageSignal) {
                    OnMessageSignal<MSG> wrapper = (OnMessageSignal<MSG>) message;
                    receiver.receive(wrapper.getMessage());
                } else {
                    log.error("unknown InBoxMessage >>>> {}", message);
                }
            } catch (Exception e) {
                log.error("", e);
            }

            synchronized (this) {
                if (!enableConcurrent && activeThreads != 1) {
                    activeThreads--;
                    return;
                }
                message = mail.poll();
                if (Objects.isNull(message)) {
                    activeThreads--;
                    return;
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!stopped) {
                stopped = true;
                mail.add(OnStopSignal.INSTANCE);
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------

    /**
     * 内部消息
     *
     * @author huangjianqin
     * @date 2020-04-16
     */
    static class InBoxMessage {
    }

    /**
     * 封装真正的message的InBoxMessage
     *
     * @author huangjianqin
     * @date 2020-04-17
     */
    final static class OnMessageSignal<M> extends InBoxMessage {
        private final M message;

        OnMessageSignal(M message) {
            this.message = message;
        }


        public M getMessage() {
            return message;
        }
    }

    /**
     * 启动InBoxMessage
     *
     * @author huangjianqin
     * @date 2020-04-16
     */
    final static class OnStartSignal extends InBoxMessage {
        static final InBoxMessage INSTANCE = new OnStartSignal();
    }

    /**
     * shutdown InBoxMessage
     *
     * @author huangjianqin
     * @date 2020-04-16
     */
    final static class OnStopSignal extends InBoxMessage {
        static final InBoxMessage INSTANCE = new OnStopSignal();
    }
}
