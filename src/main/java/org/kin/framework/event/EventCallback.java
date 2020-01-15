package org.kin.framework.event;

import org.kin.framework.utils.ExceptionUtils;

/**
 * @author huangjianqin
 * @date 2020-01-14
 */
public interface EventCallback {
    EventCallback EMPTY = new EventCallback() {
        @Override
        public void finish(Object result) {

        }

        @Override
        public void exception(Throwable throwable) {
            ExceptionUtils.log(throwable);
        }
    };

    void finish(Object result);

    void exception(Throwable throwable);
}
