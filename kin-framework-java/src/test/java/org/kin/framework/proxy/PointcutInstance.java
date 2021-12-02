package org.kin.framework.proxy;

/**
 * @author huangjianqin
 * @date 2021/12/2
 */
public class PointcutInstance {
    @Pointcut
    public void print(String str) {
        System.out.println(str);
    }

    public void sleep() throws InterruptedException {
        Thread.sleep(2_000);
    }
}
