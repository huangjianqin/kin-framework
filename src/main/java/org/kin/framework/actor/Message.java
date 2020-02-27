package org.kin.framework.actor;

/**
 * @author huangjianqin
 * @date 2018/6/5
 */
@FunctionalInterface
public interface Message<A extends Actor<?>> {
    /**
     * @param actor 处理该时间的actor
     */
    void handle(A actor);
}
