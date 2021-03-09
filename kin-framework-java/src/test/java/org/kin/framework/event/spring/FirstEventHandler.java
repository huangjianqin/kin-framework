package org.kin.framework.event.spring;

import org.kin.framework.event.FirstEvent;
import org.kin.framework.event.HandleEvent;
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
