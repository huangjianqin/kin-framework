package org.kin.framework.actor;

/**
 * Created by huangjianqin on 2018/6/5.
 */
@FunctionalInterface
public interface Message<AA extends Actor<AA>> {
    void handle(AA actor);
}
