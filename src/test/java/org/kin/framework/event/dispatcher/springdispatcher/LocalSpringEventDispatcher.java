package org.kin.framework.event.dispatcher.springdispatcher;

import org.kin.framework.event.impl.SpringEventDispatcher;
import org.kin.framework.utils.SysUtils;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2019/7/19
 */
@Component
public class LocalSpringEventDispatcher extends SpringEventDispatcher {
    public LocalSpringEventDispatcher() {
        super(SysUtils.getSuitableThreadNum());
    }
}
