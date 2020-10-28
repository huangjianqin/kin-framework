package org.kin.framework.concurrent.actor;

/**
 * @author huangjianqin
 * @date 2018/6/5
 */
@FunctionalInterface
public interface Message<TS extends PinnedThreadSafeHandler<?>> {
    /**
     * @param threadSafeHandler 处理该时间的PinnedThreadSafeHandler
     */
    void handle(TS threadSafeHandler);
}
