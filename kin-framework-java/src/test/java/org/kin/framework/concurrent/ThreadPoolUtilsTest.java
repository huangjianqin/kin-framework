package org.kin.framework.concurrent;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/10/15
 */
public class ThreadPoolUtilsTest {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtils.threadPoolBuilder().build();
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadPoolUtils.scheduledThreadPoolBuilder().build();

        threadPoolExecutor.execute(() -> System.out.println("this is task"));
        scheduledThreadPoolExecutor.schedule(() -> System.out.println("this is  schedule task"), 1, TimeUnit.SECONDS);

        Thread.sleep(2_000);
        threadPoolExecutor.shutdown();
        scheduledThreadPoolExecutor.shutdown();
    }
}
