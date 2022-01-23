package org.kin.framework.concurrent;

import com.google.common.base.Stopwatch;
import org.kin.framework.utils.TimeUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2020/11/23
 */
public class MultiThreadEventLoopGroupTest {
    private static volatile int counter = 0;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutionContext executionContext = ExecutionContext.cache("worker");
        MultiThreadEventLoopGroup eventLoopGroup = new MultiThreadEventLoopGroup(5);
        SingleThreadEventLoop eventLoop = eventLoopGroup.next();

        Stopwatch watch = Stopwatch.createStarted();

        eventLoop.execute(() -> {
            System.out.println(Thread.currentThread() + ">>>" + TimeUtils.timestamp());
            eventLoop.schedule(() -> System.out.println(TimeUtils.timestamp() + "---1"), 10, TimeUnit.SECONDS);
            eventLoop.schedule(p -> System.out.println(TimeUtils.timestamp() + "---2"), 25_000, TimeUnit.MILLISECONDS);
        });

        AtomicInteger successCounter = new AtomicInteger();
        for (int j = 0; j < 5; j++) {
            executionContext.execute(() -> {
                for (int i = 0; i < 1_000_000; i++) {
                    eventLoop.execute(() -> add());
                    eventLoop.receive(p -> add());
                    eventLoop.schedule(() -> add(), 1, TimeUnit.SECONDS);
                    eventLoop.schedule(p -> add(), 1, TimeUnit.SECONDS);

                    successCounter.addAndGet(4);
                }
            });
        }

        Thread.sleep(10_000);

        while (counter != successCounter.get()) {
            Thread.sleep(5_000);
            System.out.println(counter);
            System.out.println(successCounter.get());
            System.out.println("---------------");
        }

        System.out.println(counter);
        System.out.println(successCounter.get());

        eventLoop.shutdown();
        executionContext.shutdown();
        eventLoopGroup.shutdown();

        watch.stop();
        System.out.println(String.format("耗时: %d秒", watch.elapsed(TimeUnit.SECONDS)));
    }

    private static void add() {
        counter += 1;
    }
}
