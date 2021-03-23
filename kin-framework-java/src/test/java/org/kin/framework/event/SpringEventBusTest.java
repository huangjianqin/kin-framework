package org.kin.framework.event;

import org.kin.framework.utils.SysUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * Created by huangjianqin on 2019/3/30.
 */
@HandleEvent
@Configuration
public class SpringEventBusTest {

    @Bean
    public SpringEventBus eventBus() {
        SpringEventBus bus = new SpringEventBus(SysUtils.getSuitableThreadNum(), true);
        bus.register(new FirstEventHandler());
        return bus;
    }

    @Order(100)
    @EventFunction
    public void handleFirstEvent(FirstEvent event) {
        System.out.println("priority handle " + event.toString());
    }

    @EventFunction
    public void handleSecondEvent(SecondEvent event) {
        System.out.println("handle " + event.toString());
    }

    @EventFunction
    public void handleThirdEvent(List<ThirdEvent> events) {
        System.out.println("handle " + events);
    }

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext context = new AnnotationConfigApplicationContext("org.kin.framework.event");
        SpringEventBus bus = context.getBean(SpringEventBus.class);

        bus.post(new FirstEvent());
        bus.post(new SecondEvent());

        for (int i = 0; i < 8; i++) {
            bus.post(new ThirdEvent());
            Thread.sleep(200);
        }

        Thread.sleep(5_000);
        System.exit(0);
    }
}
