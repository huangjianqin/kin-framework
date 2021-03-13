package org.kin.framework.event;

/**
 * Created by 健勤 on 2017/8/9.
 */
@EventMerge(window = 1000)
public class ThirdEvent {
    @Override
    public String toString() {
        return "third event";
    }
}
