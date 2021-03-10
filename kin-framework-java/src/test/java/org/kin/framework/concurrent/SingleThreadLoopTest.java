package org.kin.framework.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020/11/23
 */
public class SingleThreadLoopTest {
    private static volatile int counter = 0;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutionContext executionContext = ExecutionContext.cache("worker");
        SingleThreadEventLoopGroup eventLoopGroup = new SingleThreadEventLoopGroup(5);
        SingleThreadEventLoop eventLoop = eventLoopGroup.next();

        AtomicInteger successCounter = new AtomicInteger();
        for (int j = 0; j < 5; j++) {
            executionContext.execute(() -> {
                for (int i = 0; i < 1000000; i++) {
                    eventLoop.execute(() -> add());
                    eventLoop.receive(p -> add());
                    eventLoop.schedule(() -> add(), 1, TimeUnit.SECONDS);
                    eventLoop.schedule(p -> add(), 1, TimeUnit.SECONDS);

                    successCounter.addAndGet(4);
                }
            });
        }
        Thread.sleep(150_000);

        System.out.println(counter == successCounter.get());
        System.out.println(counter);
        System.out.println(successCounter.get());

        eventLoop.shutdown();
        executionContext.shutdown();
        eventLoopGroup.shutdown();
    }

    private static void add() {
        counter += 1;
    }
}
