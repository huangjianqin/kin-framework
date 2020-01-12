package org.kin.framework.actor;

import org.kin.framework.concurrent.ThreadManager;

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
        SimpleActorLike actorLike = new SimpleActorLike();
        actorLike.tell((actor) -> {
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

    static class SimpleActorLike extends ActorLike<SimpleActorLike>{

        public SimpleActorLike() {
            super(new ThreadManager(ForkJoinPool.commonPool()));
        }
    }
}
