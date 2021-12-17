package org.kin.framework.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2021/12/12
 */
public class VarIntStreamTest {
    public static void main(String[] args) throws IOException {
        int a = Integer.MIN_VALUE;
        int b = Integer.MIN_VALUE / 2;
        int c = 0;
        int d = Integer.MAX_VALUE / 2;
        int e = Integer.MAX_VALUE;
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        VarIntUtils.writeRawVarInt32(baos1, a);
        VarIntUtils.writeRawVarInt32(baos1, b);
        VarIntUtils.writeRawVarInt32(baos1, c);
        VarIntUtils.writeRawVarInt32(baos1, d);
        VarIntUtils.writeRawVarInt32(baos1, e);
        baos1.close();

        ByteArrayInputStream bais1 = new ByteArrayInputStream(baos1.toByteArray());
        System.out.println(VarIntUtils.readRawVarInt32(bais1));
        System.out.println(VarIntUtils.readRawVarInt32(bais1));
        System.out.println(VarIntUtils.readRawVarInt32(bais1));
        System.out.println(VarIntUtils.readRawVarInt32(bais1));
        System.out.println(VarIntUtils.readRawVarInt32(bais1));

        System.out.println("-------------------------------------------");

        long a1 = Long.MIN_VALUE;
        long b1 = Long.MIN_VALUE / 2;
        long c1 = 0;
        long d1 = Long.MAX_VALUE / 2;
        long e1 = Long.MAX_VALUE;
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        VarIntUtils.writeRawVarInt64(baos2, a1);
        VarIntUtils.writeRawVarInt64(baos2, b1);
        VarIntUtils.writeRawVarInt64(baos2, c1);
        VarIntUtils.writeRawVarInt64(baos2, d1);
        VarIntUtils.writeRawVarInt64(baos2, e1);
        baos2.close();

        ByteArrayInputStream bais2 = new ByteArrayInputStream(baos1.toByteArray());
        System.out.println(VarIntUtils.readRawVarInt64(bais2));
        System.out.println(VarIntUtils.readRawVarInt64(bais2));
        System.out.println(VarIntUtils.readRawVarInt64(bais2));
        System.out.println(VarIntUtils.readRawVarInt64(bais2));
        System.out.println(VarIntUtils.readRawVarInt64(bais2));
    }
}
