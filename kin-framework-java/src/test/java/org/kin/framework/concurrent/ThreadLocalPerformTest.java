package org.kin.framework.concurrent;

import com.google.common.base.Stopwatch;
import org.kin.framework.utils.SysUtils;
import org.kin.framework.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author huangjianqin
 * @date 2020/12/9
 */
public class ThreadLocalPerformTest {
    //累计调用时间
    private static AtomicLong c1 = new AtomicLong();
    //累计query时间
    private static AtomicLong c11 = new AtomicLong();

    //累计调用时间
    private static AtomicLong c2 = new AtomicLong();
    //累计query时间
    private static AtomicLong c22 = new AtomicLong();

    //thread local num
    private static int num = 100;
    //thread local query num
    private static int query = 10000;
    //task num
    private static int taskNum = 1 * 10000;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService ec1 = Executors.newFixedThreadPool(SysUtils.getSuitableThreadNum());
        ExecutorService ec2 = Executors.newFixedThreadPool(SysUtils.getSuitableThreadNum(), new FastThreadLocalThreadFactory("netty"));

        for (int i = 0; i < taskNum; i++) {
            ec1.execute(ThreadLocalPerformTest::java);
            ec2.execute(ThreadLocalPerformTest::netty);
        }

        ec1.shutdown();
        ec2.shutdown();

        int now = TimeUtils.timestamp();
        while (!ec1.isTerminated() || !ec2.isTerminated()) {
            Thread.sleep(3 * 1000);
            if (TimeUtils.timestamp() - now % 60 == 0) {
                System.out.print(".");
            }
        }
        System.out.println();

        double javaR1 = 1.0 * c1.get() / taskNum;
        double nettyR1 = 1.0 * c2.get() / taskNum;
        System.out.println("java: " + javaR1);
        System.out.println("netty: " + nettyR1);
        System.out.println(javaR1 > nettyR1);

        double javaR2 = 1.0 * c11.get() / taskNum;
        double nettyR2 = 1.0 * c22.get() / taskNum;
        System.out.println("java query: " + javaR2);
        System.out.println("netty query: " + nettyR2);
        System.out.println(javaR2 > nettyR2);
    }

    private static void java() {
        Stopwatch w1 = Stopwatch.createStarted();

        List<ThreadLocal> list = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
            threadLocal.set(num);
            list.add(threadLocal);
        }

        Stopwatch w2 = Stopwatch.createStarted();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < query; i++) {
            int index = random.nextInt(num);
            list.get(index).get();
        }
        w2.stop();

        for (ThreadLocal threadLocal : list) {
            threadLocal.remove();
        }

        w1.stop();

        c1.addAndGet(w1.elapsed(TimeUnit.MILLISECONDS));
        c11.addAndGet(w2.elapsed(TimeUnit.MILLISECONDS));
    }

    private static void netty() {
        Stopwatch w1 = Stopwatch.createStarted();

        List<FastThreadLocal> list = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            FastThreadLocal<Integer> threadLocal = new FastThreadLocal<>();
            threadLocal.set(num);
            list.add(threadLocal);
        }

        Stopwatch w2 = Stopwatch.createStarted();
//        FastThreadLocalRandom random = FastThreadLocalRandom.current();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < query; i++) {
            int index = random.nextInt(num);
            list.get(index).get();
        }
        w2.stop();

        for (FastThreadLocal threadLocal : list) {
            threadLocal.remove();
        }

        w1.stop();

        c2.addAndGet(w1.elapsed(TimeUnit.MILLISECONDS));
        c22.addAndGet(w2.elapsed(TimeUnit.MILLISECONDS));
    }
}
