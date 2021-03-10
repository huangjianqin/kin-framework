package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2018/6/5
 */
@FunctionalInterface
public interface Message<EL extends EventLoop<EL>> {
    /**
     * @param eventLoop 处理该消息的{@link EventLoop}
     */
    void handle(EL eventLoop);
}
