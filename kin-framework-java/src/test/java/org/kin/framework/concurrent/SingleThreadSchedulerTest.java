package org.kin.framework.concurrent;

import org.kin.framework.utils.TimeUtils;

import java.util.Date;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2020/11/23
 */
public class SingleThreadSchedulerTest {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        SingleThreadEventExecutor scheduledExecutor = new SingleThreadEventExecutor(executorService);

        try {
            ScheduledFuture<?> f1 = scheduledExecutor.scheduleAtFixedRate(() -> System.out.println("rate>> ".concat(TimeUtils.formatDateTime(new Date()))),
                    2, 1, TimeUnit.SECONDS);
            ScheduledFuture<?> f2 = scheduledExecutor.scheduleWithFixedDelay(() -> System.out.println("delay>> ".concat(TimeUtils.formatDateTime(new Date()))),
                    0, 2, TimeUnit.SECONDS);
            ScheduledFuture<?> f3 = scheduledExecutor.schedule(() -> System.out.println("长任务"), 10, TimeUnit.MINUTES);
            Thread.sleep(10000);
            f1.cancel(true);
            f2.cancel(true);
            while (!f3.isDone()) {
                Thread.sleep(1000 * 60 * 1);
                scheduledExecutor.schedule(() -> System.out.println(TimeUtils.formatDateTime(new Date()).concat("--长任务等待中执行小任务")), 10, TimeUnit.SECONDS);
            }
        } finally {
            scheduledExecutor.shutdown();
            Thread.sleep(100);
            System.out.println("状态:" + scheduledExecutor.isShutdown());
            executorService.shutdown();
            Thread.sleep(100);
            System.out.println("状态:" + scheduledExecutor.isTerminated());
        }
    }
}
