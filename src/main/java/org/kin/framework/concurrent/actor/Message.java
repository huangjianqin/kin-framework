package org.kin.framework.concurrent.actor;

/**
 * @author huangjianqin
 * @date 2018/6/5
 */
@FunctionalInterface
public interface Message<TS extends PinnedThreadSafeHandler<?>> {
    /**
     * @param actor 处理该时间的actor
     */
    void handle(TS actor);
}
