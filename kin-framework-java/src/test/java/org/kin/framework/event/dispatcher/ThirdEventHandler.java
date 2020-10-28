package org.kin.framework.event.dispatcher;

import org.kin.framework.event.EventHandler;

/**
 * @author huangjianqin
 * @date 2020-01-12
 */
public class ThirdEventHandler implements EventHandler<ThirdEvent> {

    @Override
    public void handle(ThirdEvent event) {
        System.out.println("handle " + event.getType());
    }
}
