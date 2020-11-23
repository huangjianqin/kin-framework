package org.kin.framework.concurrent;

import org.kin.framework.utils.TimeUtils;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/11/23
 */
public class StScheduledExecutorTest {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        StScheduledExecutor scheduledExecutor = new StScheduledExecutor(executorService);

        try {
            scheduledExecutor.scheduleAtFixedRate(() -> System.out.println("rate>> ".concat(TimeUtils.formatDateTime(new Date()))),
                    0, 1, TimeUnit.SECONDS);
            scheduledExecutor.scheduleWithFixedDelay(() -> System.out.println("delay>> ".concat(TimeUtils.formatDateTime(new Date()))),
                    0, 2, TimeUnit.SECONDS);
            Thread.sleep(10000);
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
