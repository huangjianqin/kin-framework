package org.kin.framework.proxy;

/**
 * @author huangjianqin
 * @date 2021/12/2
 */
public class ByteBuddyAdviceTest {
    public static void main(String[] args) throws InterruptedException {
        AdviceDefinition adviceDefinition = new AdviceDefinition().include("org.kin.framework.proxy").adviceAndPointcutAnnotation(TimeLogAdvice.class, Pointcut.class);
        ByteBuddys.installAdvices(adviceDefinition);
        PointcutInstance instance = new PointcutInstance();

        instance.print("hello");
        instance.sleep();
    }
}
