package org.kin.framework.event.dispatcher;

import org.kin.framework.event.EventHandler;

/**
 * @author huangjianqin
 * @date 2020-01-12
 */
public class FirstEventHandler implements EventHandler<FirstEvent> {

    @Override
    public void handle(FirstEvent event) {
        System.out.println("handle " + event.getType());
    }
}
