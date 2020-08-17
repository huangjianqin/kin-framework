package org.kin.framework.event.dispatcher;

import org.kin.framework.event.Event;

/**
 * Created by 健勤 on 2017/8/9.
 */
@Event
public class SecondEvent {
    private SecondEventType type;

    public SecondEvent(SecondEventType type) {
        this.type = type;
    }

    public SecondEventType getType() {
        return type;
    }
}
