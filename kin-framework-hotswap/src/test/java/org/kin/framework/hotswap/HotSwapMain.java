package org.kin.framework.hotswap;

/**
 * 启动后观察一会输出, 然后将hotswap/Test.class复制到classes目录下, 则可以看到输出从222变成111
 * Created by huangjianqin on 2018/10/31.
 */
public class HotSwapMain {
    public static void main(String[] args) {
        Test test = new Test();
        FileMonitor monitor = FileMonitor.common();
        int i = 0;
        while (true) {
            try {
                Thread.sleep(5000);
                System.out.println(test.message());
                i++;
                if (i % 10 == 0) {
                    test = new Test();
                    System.out.println("new obj");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
