package org.kin.framework.actor;

/**
 * 启动InBoxMessage
 *
 * @author huangjianqin
 * @date 2020-04-16
 */
final class OnStartSignal extends InBoxMessage {
    static final InBoxMessage INSTANCE = new OnStartSignal();
}
