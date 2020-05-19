package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/9
 */
public class TestPinnedThreadSafeHandler {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        PinnedThreadSafeHandler<?> threadSafeHandler = new PinnedThreadSafeHandler(new ExecutionContext(ForkJoinPool.commonPool()));
        threadSafeHandler.handle((actor) -> {
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
