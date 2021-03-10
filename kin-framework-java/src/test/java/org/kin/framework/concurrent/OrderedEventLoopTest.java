package org.kin.framework.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2019/7/9
 */
@SuppressWarnings("ALL")
public class OrderedEventLoopTest {
    private static volatile int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        int num = 10;
        ExecutionContext executionContext = ExecutionContext.cache("worker", 1, "worker_scheudle");
        FixOrderedEventLoopGroup pool = new FixOrderedEventLoopGroup(num, executionContext, OrderedEventLoop::new);

        OrderedEventLoop next = pool.next(0);

        AtomicInteger successCounter = new AtomicInteger();
        for (int j = 0; j < 5; j++) {
            executionContext.execute(() -> {
                for (int i = 0; i < 1000000; i++) {
                    next.execute(() -> add());
                    next.receive(p -> add());
                    next.schedule(() -> add(), 1, TimeUnit.SECONDS);
                    next.schedule(p -> add(), 1, TimeUnit.SECONDS);

                    successCounter.addAndGet(4);
                }
            });
        }
        Thread.sleep(60_000);

        System.out.println(counter == successCounter.get());
        System.out.println(counter);
        System.out.println(successCounter.get());
        pool.shutdown();
    }

    private static void add() {
        counter += 1;
    }
}
