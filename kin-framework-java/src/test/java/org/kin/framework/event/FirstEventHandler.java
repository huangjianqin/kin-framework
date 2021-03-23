package org.kin.framework.event;

/**
 * @author huangjianqin
 * @date 2020-01-12
 */
public class FirstEventHandler implements EventHandler<FirstEvent> {
    @Override
    public void handle(EventBus bus, FirstEvent event) throws Exception {
        System.out.println("handle " + event.toString());
    }
}
