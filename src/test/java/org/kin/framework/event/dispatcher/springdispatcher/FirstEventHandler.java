package org.kin.framework.event.dispatcher.springdispatcher;

import org.kin.framework.event.annotation.HandleEvent;
import org.kin.framework.event.dispatcher.FirstEvent;
import org.springframework.stereotype.Component;

/**
 * Created by huangjianqin on 2019/3/30.
 */
@Component
@HandleEvent
public class FirstEventHandler {
    public void handle(FirstEvent event) {
        System.out.println("handle " + event);
    }
}
