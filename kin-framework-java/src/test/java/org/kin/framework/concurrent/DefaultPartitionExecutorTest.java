package org.kin.framework.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2020-05-18
 */
public class DefaultPartitionExecutorTest {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutionContext executionContext = ExecutionContext.fix(10, "dispatcher-test");

        Partitioner<Integer> partitioner = (key, numPartition) -> key % numPartition;

        int partition = 5;
        DefaultPartitionExecutor<Integer> executor = new DefaultPartitionExecutor<>(partition, partitioner);
        int num = 1000;
        Map<Integer, Set<Integer>> counter = new HashMap<>();
        for (int i = 0; i < partition; i++) {
            counter.put(i, new ConcurrentSkipListSet<>());
        }

        CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            int finalI = i;
            executionContext.execute(() -> executor.execute(finalI, () -> {
                int key = finalI % partition;
                counter.get(key).add(finalI);
                latch.countDown();
            }));
        }

        latch.await();

        //schedule
        Callable<String> callable = () -> System.currentTimeMillis() + "--" + ThreadLocalRandom.current().nextInt(num);
        Future<String> future = executor.schedule(1, callable, 3, TimeUnit.SECONDS);
        System.out.println(future.get());

        Thread.sleep(2_980);

        executionContext.shutdown();
        executor.shutdown();

        counter.forEach((key, value) -> System.out.println(key + ">>>>" + value));
    }
}
