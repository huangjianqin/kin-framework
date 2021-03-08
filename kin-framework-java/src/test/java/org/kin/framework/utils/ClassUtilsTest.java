package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2021/3/8
 */
public class ClassUtilsTest {
    public static void main(String[] args) {
        //根据genericType获取泛型类型
        System.out.println(ClassUtils.getInferredClassForGeneric(Printer.class.getMethods()[2].getGenericReturnType()));
        ;
    }

    public interface Printable<C> {
        default void println(C c) {
            System.out.println(c);
        }

        Printable<C> create();
    }

    public static class Printer implements Printable<Integer> {
        @Override
        public void println(Integer integer) {
            System.out.println("int >>> " + integer);
        }

        @Override
        public Printable<Integer> create() {
            return new Printer();
        }
    }
}
