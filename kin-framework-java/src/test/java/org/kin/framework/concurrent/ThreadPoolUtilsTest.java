package org.kin.framework.concurrent;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/10/15
 */
public class ThreadPoolUtilsTest {
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtils.threadPoolBuilder().common();
        EagerThreadPoolExecutor eagerThreadPoolExecutor = ThreadPoolUtils.threadPoolBuilder().eager();
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = ThreadPoolUtils.scheduledThreadPoolBuilder().build();
        ForkJoinPool forkJoinPool = ThreadPoolUtils.forkJoinThreadPoolBuilder().build();

        threadPoolExecutor.execute(() -> System.out.println("this is task"));
        eagerThreadPoolExecutor.execute(() -> System.out.println("this is eager task"));
        scheduledThreadPoolExecutor.schedule(() -> System.out.println("this is schedule task"), 1, TimeUnit.SECONDS);
        forkJoinPool.execute(() -> System.out.println("this is forkjoin task"));

        Thread.sleep(2_000);
        threadPoolExecutor.shutdown();
        eagerThreadPoolExecutor.shutdown();
        scheduledThreadPoolExecutor.shutdown();
        forkJoinPool.shutdown();
    }
}
