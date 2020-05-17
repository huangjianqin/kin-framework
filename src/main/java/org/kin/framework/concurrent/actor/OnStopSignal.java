package org.kin.framework.concurrent.actor;

/**
 * shutdown InBoxMessage
 *
 * @author huangjianqin
 * @date 2020-04-16
 */
final class OnStopSignal extends InBoxMessage {
    static final InBoxMessage INSTANCE = new OnStopSignal();
}
