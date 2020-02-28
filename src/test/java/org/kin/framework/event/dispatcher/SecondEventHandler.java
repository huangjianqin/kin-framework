package org.kin.framework.event.dispatcher;

import org.kin.framework.event.annotation.HandleEvent;

/**
 * @author huangjianqin
 * @date 2020-01-12
 */
@HandleEvent
public class SecondEventHandler{

    public void handle(SecondEvent event) {
        System.out.println("handle " + event.getType());
    }
}