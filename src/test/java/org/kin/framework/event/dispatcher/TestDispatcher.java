package org.kin.framework.event.dispatcher;


import org.kin.framework.event.EventCallback;
import org.kin.framework.event.impl.EventDispatcher;
import org.kin.framework.utils.SysUtils;

/**
 * Created by 健勤 on 2017/8/10.
 */
public class TestDispatcher {
    public static void main(String[] args) throws InterruptedException {
        EventDispatcher dispatcher = new EventDispatcher(SysUtils.getSuitableThreadNum(), true);
//        dispatcher.register(FirstEvent.class, new FirstEventHandler(), FirstEventHandler.class.getMethods()[0]);
        dispatcher.register(SecondEvent.class, new SecondEventHandler(), SecondEventHandler.class.getMethods()[0]);
        dispatcher.register(ThirdEvent.class, new ThirdEventHandler(), ThirdEventHandler.class.getMethods()[0]);

        dispatcher.serviceInit();
        dispatcher.serviceStart();
        dispatcher.asyncDispatch(new SecondEvent(SecondEventType.S), new EventCallback() {
            @Override
            public void finish(Object result) {
                System.out.println(result);
            }

            @Override
            public void exception(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        dispatcher.dispatch(new SecondEvent(SecondEventType.C));
        dispatcher.dispatch(new SecondEvent(SecondEventType.D));

        Thread.sleep(2000);

        dispatcher.serviceStop();
    }
}
