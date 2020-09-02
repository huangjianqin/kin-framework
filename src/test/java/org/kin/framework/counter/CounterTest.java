package org.kin.framework.counter;

import org.kin.framework.concurrent.ExecutionContext;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020/9/2
 */
public class CounterTest {
    public static void main(String[] args) throws InterruptedException {
        List<String> counterKeies = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        int parallelism = 10;
        long sum = 1000000;
        CountDownLatch latch = new CountDownLatch(parallelism);
        ExecutionContext worker = ExecutionContext.forkjoin(parallelism, "worker");
        for (int i = 0; i < parallelism; i++) {
            worker.execute(() -> {
                for (long l = 0; l < sum / parallelism; l++) {
                    String group = ThreadLocalRandom.current().nextInt(parallelism) + "";
                    String counter = counterKeies.get(ThreadLocalRandom.current().nextInt(parallelism));
                    Counters.increment(group, counter);
                }
                latch.countDown();
            });
        }
        latch.await();
        System.out.println(Reporters.report());
    }
}
