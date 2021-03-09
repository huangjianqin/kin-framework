package org.kin.framework.event;

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
