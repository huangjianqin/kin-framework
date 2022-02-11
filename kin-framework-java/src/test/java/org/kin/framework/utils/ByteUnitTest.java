package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2019/7/31
 */
public class ByteUnitTest {
    public static void main(String[] args) {
        System.out.println(ByteUnit.convert(1, ByteUnit.KILOBYTE, ByteUnit.BYTE));
        System.out.println(ByteUnit.convert(1, ByteUnit.MEGABYTE, ByteUnit.BYTE));
        System.out.println(ByteUnit.convert(1, ByteUnit.GIGABYTE, ByteUnit.BYTE));
        System.out.println(ByteUnit.convert(1, ByteUnit.TERABYTE, ByteUnit.BYTE));
        System.out.println(ByteUnit.convert(1, ByteUnit.PETABYTE, ByteUnit.BYTE));

        System.out.println("--------------------");
        System.out.println(ByteUnit.convert(1000, ByteUnit.BYTE, ByteUnit.KILOBYTE));

        System.out.println("--------------------");
        System.out.println(ByteUnit.format(100));
        System.out.println(ByteUnit.format(1024));
        System.out.println(ByteUnit.format(1500));
        System.out.println(ByteUnit.format(1024 * 1024 - 1));
        System.out.println(ByteUnit.format(1024 * 1024 + 1));
    }
}
