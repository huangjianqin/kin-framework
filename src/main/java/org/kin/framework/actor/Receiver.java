package org.kin.framework.actor;

/**
 * @author huangjianqin
 * @date 2020-04-16
 */
public abstract class Receiver<MSG> {
    public abstract void receive(MSG mail);
}
