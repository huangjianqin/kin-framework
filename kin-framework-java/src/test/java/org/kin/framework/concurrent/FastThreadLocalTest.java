package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2020/11/17
 */
public class FastThreadLocalTest {
    private static final FastThreadLocal<Integer> threadLocal = new FastThreadLocal<>();

    public static void main(String[] args) {
        ExecutionContext ec = ExecutionContext.fix(5, new FastThreadLocalThreadFactory("fastThreadLocal"));
        int count = 100;
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                //双数才设置并访问
                int finalI = i / 10;
                ec.execute(() -> {
                    System.out.println(Thread.currentThread().getName() + "设置threadlocal成功 >>" + finalI);
                    threadLocal.set(finalI);
                });
            } else {
                //单数打印
                ec.execute(() -> {
                    System.out.println(Thread.currentThread().getName() + ">>" + threadLocal.get());
                });
            }
        }

        ec.shutdownNow();
    }
}
