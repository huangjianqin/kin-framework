package org.kin.framework.concurrent;

import org.kin.framework.utils.TimeUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2022/1/7
 */
public class LevelWheelTimerTest {
    public static void main(String[] args) throws InterruptedException {
        LevelWheelTimer wheelTimer = new LevelWheelTimer(10, 60);

        Timeout test1 = wheelTimer.newTimeout(t -> System.out.println(TimeUtils.timestamp() + "-test1"), 2, TimeUnit.SECONDS);
        Timeout test2 = wheelTimer.newTimeout(t -> System.out.println(TimeUtils.timestamp() + "-test2"), 2, TimeUnit.SECONDS);
        Timeout test3 = wheelTimer.newTimeout(t -> System.out.println(TimeUtils.timestamp() + "-test3"), 2, TimeUnit.SECONDS);
        Timeout test4 = wheelTimer.newTimeout(t -> System.out.println(TimeUtils.timestamp() + "-test4"), 10, TimeUnit.SECONDS);

        test2.cancel();
        System.out.println(TimeUtils.timestamp());
        System.out.println("test2 cancelled");
        Thread.sleep(5_000);
        System.out.println(wheelTimer.stop());
        ;
    }
}
