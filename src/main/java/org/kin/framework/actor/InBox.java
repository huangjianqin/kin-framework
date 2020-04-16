package org.kin.framework.actor;

import org.kin.framework.Closeable;
import org.kin.framework.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-04-15
 */
public class InBox<MSG> implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(InBox.class);

    private final Receiver<MSG> receiver;
    private final boolean enableConcurrent;

    private LinkedList<MSG> mail = new LinkedList<>();
    private int activeThreads = 0;
    private boolean stopped;

    public InBox(Receiver receiver, boolean enableConcurrent) {
        this.receiver = receiver;
        this.enableConcurrent = enableConcurrent;
    }

    public void post(MSG message) {
        synchronized (this) {
            if (stopped) {
                log.warn(String.format("Drop %s because %s is stopped", message, receiver));
                return;
            }
            mail.add(message);
        }
    }

    public void process(Dispatcher dispatcher) {
        MSG message = null;
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
                receiver.receive(message);
            } catch (Exception e) {
                ExceptionUtils.log(e);
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
            }
        }
    }
}
