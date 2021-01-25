package org.kin.framework.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;

/**
 * @author huangjianqin
 * @date 2020-05-18
 */
public class PartitionTaskExecutorTest {
    public static void main(String[] args) throws InterruptedException {
        ExecutionContext executionContext = ExecutionContext.fix(10, "dispatcher-test");

        Partitioner<Integer> partitioner = (key, numPartition) -> key % numPartition;

        int partition = 5;
        PartitionTaskExecutor<Integer> executor = new PartitionTaskExecutor<>(partition, partitioner);
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
//        while (true) {
//            counter.forEach((key, value) -> System.out.println(value.size()));
//            Thread.sleep(2000);
//            System.out.println("----------------------------");
//        }
        latch.await();
        executionContext.shutdown();
        executor.shutdown();

        counter.forEach((key, value) -> System.out.println(key + ">>>>" + value));
    }
}
