package org.kin.framework.event.simplelistener;

import org.kin.framework.listener.SimpleListenerManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.List;

/**
 * Created by huangjianqin on 2019/3/1.
 */
public class SimpleListenerManagerTest {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new FileSystemXmlApplicationContext("classpath:application.xml");
        SimpleListenerManager listenerManager = applicationContext.getBean(SimpleListenerManager.class);
        List<Listener1> listener1s = listenerManager.getListener(Listener1.class);
        for (Listener1 listener1 : listener1s) {
            listener1.call();
        }
        System.exit(0);
    }
}









