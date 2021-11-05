package org.kin.framework.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2021/11/5
 */
public class ObjectPoolTest {
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    public static void main(String[] args) throws InterruptedException {
        ObjectPool<PriObject> pool = ObjectPool.newPool(h -> new PriObject(h, ID_GENERATOR.incrementAndGet()));
        System.out.println("---------------------------单线程---------------------------------------");
        //单线程
        PriObject o1 = pool.get();
        PriObject o2 = pool.get();

        System.out.println(o1);
        System.out.println(o2);

        o1.recycle();

        //o1
        PriObject o3 = pool.get();
        System.out.println(o3);
        System.out.println(o1 == o3);

        o2.recycle();

        //o2
        PriObject o4 = pool.get();
        System.out.println(o4);
        System.out.println(o2 == o4);

        //o3
        PriObject o5 = pool.get();
        System.out.println(o5);

        //全部回收
        o3.recycle();
        o4.recycle();
        o5.recycle();

        System.out.println("---------------------------多线程---------------------------------------");
        //多线程回收, 观察有没有多分配对象(根据id)
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            //o1 o2 o3
            PriObject o6 = pool.get();
            PriObject o7 = pool.get();
            PriObject o8 = pool.get();
            System.out.println(o6);
            System.out.println(o7);
            System.out.println(o8);

            CountDownLatch latch = new CountDownLatch(2);
            executor.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                o6.recycle();
                latch.countDown();
            });
            executor.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                o7.recycle();
                o8.recycle();
                latch.countDown();
            });

            latch.await();
            //o1 o2 o3
            PriObject o9 = pool.get();
            PriObject o10 = pool.get();
            PriObject o11 = pool.get();
            System.out.println(">>>");
            System.out.println(o9);
            System.out.println(o10);
            System.out.println(o11);
        } finally {
            executor.shutdown();
        }
    }

    private static class PriObject extends AbstractPooledObject {
        private final int tag = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        private int id;

        public PriObject(ObjectPool.Handle handle, int id) {
            super(handle);
            this.id = id;
        }

        //getter
        public int getTag() {
            return tag;
        }

        public int getId() {
            return id;
        }

        @Override
        protected void beforeRecycle() {
            //do nothing
        }

        @Override
        public String toString() {
            return "PriObject{" +
                    "tag=" + tag +
                    ", id=" + id +
                    "} ";
        }
    }
}
