package org.kin.framework.concurrent;

import org.kin.framework.utils.TimeUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/6/5
 */
public class SensitiveScheduledThreadPoolExecutorTest {
    public static void main(String[] args) throws InterruptedException {
        SensitiveScheduledThreadPoolExecutor scheduler = new SensitiveScheduledThreadPoolExecutor(2);
        scheduler.execute(() -> System.out.println(TimeUtils.formatDateTime()));
        scheduler.schedule(() -> System.out.println("s" + ">>>>>>" + TimeUtils.formatDateTime()), 3, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> System.out.println("sar" + ">>>>>>" + TimeUtils.formatDateTime()), 0, 5, TimeUnit.SECONDS);
        Thread.sleep(21_000);
        scheduler.shutdown();
    }
}
