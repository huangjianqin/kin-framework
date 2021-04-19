package org.kin.framework.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/4/19
 */
public class EagerThreadPoolExecutorTest {
    public static void main(String[] args) throws InterruptedException {
        int queues = 5;
        int cores = 5;
        int threads = 10;
        // alive 1 second
        long alive = 1000;

        //executor
        EagerThreadPoolExecutor executor = EagerThreadPoolExecutor.create(cores,
                threads,
                alive,
                TimeUnit.MILLISECONDS,
                queues);

        for (int i = 0; i < 15; i++) {
            Thread.sleep(50);
            executor.execute(() -> {
                int poolSize = executor.getPoolSize();
                System.out.println(
                        "thread number in current pool：" +
                                poolSize +
                                ",  task number in task queue：" +
                                executor.getQueue().size() +
                                " executor size: " +
                                poolSize);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.sleep(5000);
        System.out.println(executor.getPoolSize() == cores);

        executor.shutdown();
    }
}
