package org.kin.framework.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/9
 */
public class PinnedThreadExecutorTest {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        PinnedThreadExecutor<?> executor = new PinnedThreadExecutor(ExecutionContext.cache("worker"));
        executor.handle((actor) -> {
            System.out.println(1);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(2);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
