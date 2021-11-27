package org.kin.framework.utils.reflection;

/**
 * @author huangjianqin
 * @date 2021/11/27
 */
public class FieldUpdatersTest {
    public static void main(String[] args) {
        Class1 class1 = new Class1();
        System.out.println(class1);

        ByteFieldUpdater<Class1> f1Updater = FieldUpdaters.newByteFieldUpdater(Class1.class, "f1");
        CharFieldUpdater<Class1> f2Updater = FieldUpdaters.newCharFieldUpdater(Class1.class, "f2");
        ShortFieldUpdater<Class1> f3Updater = FieldUpdaters.newShortFieldUpdater(Class1.class, "f3");
        IntegerFieldUpdater<Class1> f4Updater = FieldUpdaters.newIntegerFieldUpdater(Class1.class, "f4");
        LongFieldUpdater<Class1> f5Updater = FieldUpdaters.newLongFieldUpdater(Class1.class, "f5");
        FloatFieldUpdater<Class1> f6Updater = FieldUpdaters.newFloatFieldUpdater(Class1.class, "f6");
        DoubleFieldUpdater<Class1> f7Updater = FieldUpdaters.newDoubleFieldUpdater(Class1.class, "f7");
        ReferenceFieldUpdater<Class1, String> f8Updater = FieldUpdaters.newReferenceFieldUpdater(Class1.class, "f8");

        f1Updater.set(class1, Byte.MAX_VALUE);
        f2Updater.set(class1, 'c');
        f3Updater.set(class1, Short.MAX_VALUE);
        f4Updater.set(class1, Integer.MAX_VALUE);
        f5Updater.set(class1, Long.MAX_VALUE);
        f6Updater.set(class1, Float.MAX_VALUE);
        f7Updater.set(class1, Double.MAX_VALUE);
        f8Updater.set(class1, "string");

        System.out.println(class1);

        ByteFieldUpdater<Class1> f11Updater = FieldUpdaters.newByteFieldUpdater(Class1.class, "f11");
        CharFieldUpdater<Class1> f12Updater = FieldUpdaters.newCharFieldUpdater(Class1.class, "f12");
        ShortFieldUpdater<Class1> f13Updater = FieldUpdaters.newShortFieldUpdater(Class1.class, "f13");
        IntegerFieldUpdater<Class1> f14Updater = FieldUpdaters.newIntegerFieldUpdater(Class1.class, "f14");
        LongFieldUpdater<Class1> f15Updater = FieldUpdaters.newLongFieldUpdater(Class1.class, "f15");
        FloatFieldUpdater<Class1> f16Updater = FieldUpdaters.newFloatFieldUpdater(Class1.class, "f16");
        DoubleFieldUpdater<Class1> f17Updater = FieldUpdaters.newDoubleFieldUpdater(Class1.class, "f17");
        ReferenceFieldUpdater<Class1, String> f18Updater = FieldUpdaters.newReferenceFieldUpdater(Class1.class, "f18");

        f11Updater.set(class1, Byte.MIN_VALUE);
        f12Updater.set(class1, 'C');
        f13Updater.set(class1, Short.MIN_VALUE);
        f14Updater.set(class1, Integer.MIN_VALUE);
        f15Updater.set(class1, Long.MIN_VALUE);
        f16Updater.set(class1, Float.MIN_VALUE);
        f17Updater.set(class1, Double.MIN_VALUE);
        f18Updater.set(class1, "STRING");

        System.out.println(class1);
    }

    private static class Class1 {
        private byte f1;
        private char f2;
        private short f3;
        private int f4;
        private float f5;
        private long f6;
        private double f7;
        private Object f8;

        private volatile byte f11;
        private volatile char f12;
        private volatile short f13;
        private volatile int f14;
        private volatile float f15;
        private volatile long f16;
        private volatile double f17;
        private volatile Object f18;

        @Override
        public String toString() {
            return "Class1{" +
                    "f1=" + f1 +
                    ", f2=" + f2 +
                    ", f3=" + f3 +
                    ", f4=" + f4 +
                    ", f5=" + f5 +
                    ", f6=" + f6 +
                    ", f7=" + f7 +
                    ", f8=" + f8 +
                    ", f11=" + f11 +
                    ", f12=" + f12 +
                    ", f13=" + f13 +
                    ", f14=" + f14 +
                    ", f15=" + f15 +
                    ", f16=" + f16 +
                    ", f17=" + f17 +
                    ", f18=" + f18 +
                    '}';
        }
    }
}
