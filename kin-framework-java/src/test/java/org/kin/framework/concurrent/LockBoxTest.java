package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2020-01-15
 */
public class LockBoxTest {
    public static void main(String[] args) throws InterruptedException {
        LockBox<Integer> lockBox = new LockBox<>();
        ExecutionContext executionContext = ExecutionContext.cache("worker");

        executionContext.execute(() -> lockBox.lockRun(1, () -> {
            System.out.println(1111);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            lockBox.lockRun(2, () -> System.out.println(11112222));
        }));

        executionContext.execute(() -> lockBox.lockRun(2, () -> {
            System.out.println(2222);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
            lockBox.lockRun(1, () -> System.out.println(22221111));
        }));

//        threadManager.execute(() -> {
//            lockBox.execute(3, () -> {
//                System.out.println(3333);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//
//                }
//                lockBox.execute(1, () -> {
//                    System.out.println(33331111);
//                });
//            });
//        });

        Thread.sleep(5000);
        executionContext.shutdown();
    }
}
