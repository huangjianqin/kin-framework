package org.kin.framework.actor;

import org.kin.framework.Closeable;

/**
 * @author huangjianqin
 * @date 2020-04-26
 */
public interface Dispatcher<KEY, MSG> extends Closeable {
    void init();

    void register(KEY key, Receiver<MSG> receiver, boolean enableConcurrent);

    void unRegister(KEY key);

    void postMessage(KEY key, MSG message);
}
