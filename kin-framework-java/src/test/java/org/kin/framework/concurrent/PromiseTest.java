package org.kin.framework.concurrent;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/**
 * @author huangjianqin
 * @date 2021/11/13
 */
public class PromiseTest {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        MultiThreadEventLoopGroup eventLoopGroup = new MultiThreadEventLoopGroup(5);

        Promise<Integer> promise1 = test(null);
        promise1.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "none executor1:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });
        promise1.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "none executor2:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });

        Promise<Integer> promise2 = test(executor);
        promise2.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "common executor1:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });
        promise2.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "common executor2:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });

        Promise<Integer> promise3 = test(eventLoopGroup.next());
        promise3.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "event executor1:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });
        promise3.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "event executor2:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });

        Thread.sleep(2_000);
        System.out.println("after 2s------------------------------------------------------------------------");
        promise1.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "none executor3:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });
        promise2.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "common executor3:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });
        promise3.addListener((p) -> {
            System.out.println(Thread.currentThread().getName() + " | " + "event executor3:::" + System.currentTimeMillis() + ">>>>>>" + p.get());
        });

        executor.shutdown();
        eventLoopGroup.shutdown();
    }

    private static Promise<Integer> test(Executor executor) {
        DefaultPromise<Integer> promise;
        if (Objects.isNull(executor)) {
            promise = new DefaultPromise<>();
        } else {
            promise = new DefaultPromise<>(executor);
        }
        ForkJoinPool.commonPool().execute(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            promise.success(1);
        });
        return promise;
    }
}
