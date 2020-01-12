package org.kin.framework.event.dispatcher.springdispatcher;

import org.kin.framework.event.dispatcher.FirstEvent;
import org.kin.framework.event.dispatcher.FirstEventType;
import org.kin.framework.event.impl.SpringEventDispatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Created by huangjianqin on 2019/3/30.
 */
public class SpringAsyncDispatcherTest {
    public static void main(String[] args) {
        ApplicationContext context = new FileSystemXmlApplicationContext("classpath:application.xml");
        SpringEventDispatcher springAsyncDispatcher = context.getBean(SpringEventDispatcher.class);
        springAsyncDispatcher.dispatch(new FirstEvent(FirstEventType.E));
        System.exit(0);
    }
}
