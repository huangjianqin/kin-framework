package org.kin.framework.concurrent.actor;

import org.kin.framework.concurrent.ExecutionContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/9
 */
public class TestActorLike {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        SimplePinnedThreadSafeHandler threadSafeHandler = new SimplePinnedThreadSafeHandler();
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

    static class SimplePinnedThreadSafeHandler extends PinnedThreadSafeHandler<SimplePinnedThreadSafeHandler> {

        public SimplePinnedThreadSafeHandler() {
            super(new ExecutionContext(ForkJoinPool.commonPool()));
        }
    }
}
