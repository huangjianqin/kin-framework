package org.kin.framework.concurrent;

import org.kin.framework.utils.TimeUtils;

/**
 * @author huangjianqin
 * @date 2021/10/11
 */
public class RepeatedTimerTest {
    public static void main(String[] args) throws InterruptedException {
        DemoRepeatedTimer timer = new DemoRepeatedTimer("demo timer", 1_000);
        System.out.println(TimeUtils.timestamp() + "------timer start");
        timer.start();
        Thread.sleep(2_000);

        System.out.println(TimeUtils.timestamp() + "------timer stop");
        timer.stop();
        Thread.sleep(2_000);

        System.out.println(TimeUtils.timestamp() + "------timer trigger once");
        timer.triggerOnceNow();
        Thread.sleep(2_000);

        System.out.println(TimeUtils.timestamp() + "------timer restart");
        timer.start();
        Thread.sleep(2_000);

        System.out.println(TimeUtils.timestamp() + "------timer destroy");
        timer.destroy();
    }

    private static class DemoRepeatedTimer extends RepeatedTimer {

        protected DemoRepeatedTimer(String name, int timeoutMs) {
            super(name, timeoutMs);
        }

        @Override
        protected void onTrigger() {
            System.out.println(TimeUtils.timestamp() + "------timer trigger");
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            System.out.println(TimeUtils.timestamp() + "------timer onDestroy");
        }
    }
}
