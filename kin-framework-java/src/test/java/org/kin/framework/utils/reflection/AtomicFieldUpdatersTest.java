package org.kin.framework.utils.reflection;

import org.kin.framework.concurrent.atom.AtomicFieldUpdaters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
public class AtomicFieldUpdatersTest {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            Class1 class1 = new Class1();
            AtomicIntegerFieldUpdater<Class1> f1Updater = AtomicFieldUpdaters.newAtomicIntegerFieldUpdater(Class1.class, "f1");
            AtomicLongFieldUpdater<Class1> f2Updater = AtomicFieldUpdaters.newAtomicLongFieldUpdater(Class1.class, "f2");

            for (int i = 0; i < 5; i++) {
                executor.execute(() -> {
                    for (int j = 0; j < 1000; j++) {
                        f1Updater.addAndGet(class1, 1);
                        f2Updater.addAndGet(class1, 2);
                    }
                });
            }

            Thread.sleep(2_000);

            System.out.println(class1.getF1() == 5000);
            System.out.println(class1.getF2() == 10000);
        } finally {
            executor.shutdown();
        }
    }

    private static class Class1 {
        private volatile int f1;
        private volatile long f2;

        //getter
        public int getF1() {
            return f1;
        }

        public long getF2() {
            return f2;
        }
    }
}
