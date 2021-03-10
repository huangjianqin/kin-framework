package org.kin.framework.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/3/10
 */
public class HashedWheelTimerTest {
    public static void main(String[] args) throws InterruptedException {
        HashedWheelTimer wheelTimer = new HashedWheelTimer();
        wheelTimer.start();

        Timeout test1 = wheelTimer.newTimeout(t -> System.out.println("test1"), 2, TimeUnit.SECONDS);
        Timeout test2 = wheelTimer.newTimeout(t -> System.out.println("test2"), 2, TimeUnit.SECONDS);
        Timeout test3 = wheelTimer.newTimeout(t -> System.out.println("test3"), 2, TimeUnit.SECONDS);

        test2.cancel();
        Thread.sleep(5_000);
        wheelTimer.stop();
    }
}
