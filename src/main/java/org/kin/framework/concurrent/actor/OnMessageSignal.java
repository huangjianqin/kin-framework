package org.kin.framework.concurrent.actor;

/**
 * 封装真正的message的InBoxMessage
 *
 * @author huangjianqin
 * @date 2020-04-17
 */
final class OnMessageSignal<MSG> extends InBoxMessage {
    private MSG message;

    OnMessageSignal(MSG message) {
        this.message = message;
    }


    public MSG getMessage() {
        return message;
    }
}
