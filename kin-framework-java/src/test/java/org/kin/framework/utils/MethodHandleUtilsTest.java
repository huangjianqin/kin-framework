package org.kin.framework.utils;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2021/3/8
 */
public class MethodHandleUtilsTest {
    public static void main(String[] args) throws Throwable {
        Printer printer = new Printer();
        Method m1 = Printable.class.getMethods()[0];
        MethodHandleUtils.invokeInterfaceDefaultMethodHandle(m1, Printable.class, printer, "哈哈");

        Method m2 = Printer.class.getMethods()[0];
        MethodHandleUtils.invokeCommonMethodHandle(m2, printer, "哈哈");

        //学习
//        System.out.println(m2);
//        MethodType methodType = MethodType.methodType(m2.getReturnType(), m2.getParameterTypes());
//        MethodHandles.Lookup lookup = MethodHandles.lookup();
//        //必须是对应类的对应方法, 哪怕是实现父类方法也不行, 第一个参数必须是对应class实例获取的对应method实例
//        //下面是Printable.class就会报错
//        MethodHandle methodHandle1 = lookup.findVirtual(m2.getDeclaringClass(), m2.getName(), methodType);
//        //会识别返回值变量, 如果返回值变量类型与对应方法返回值类型不一致, 则也会报错
//        //第一个参数是绑定的实例
//        methodHandle1.invokeExact(printer, "哈哈");
//        //会自动转换返回值变量类型, 相比invokeExact(), 哪怕返回值变量类型与对应方法返回值类型不一致, 能运行, 不会报错
//        String r = (String) methodHandle1.invoke(printer, "哈哈");
//        System.out.println(r);
//        //与invoke类似, 相比invokeExact和invoke, 支持反射中被调用
//        String r1 = (String) methodHandle1.bindTo(printer).invokeWithArguments("哈哈");
//        System.out.println(r1);
    }

    public interface Printable {
        default void println(String s) {
            System.out.println(s);
        }
    }

    public static class Printer implements Printable {
        @Override
        public void println(String s) {
            System.out.println(">>> " + s);
        }
    }
}
