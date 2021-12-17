package org.kin.framework.utils;

import org.kin.framework.io.ByteBufferUtils;

import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2021/8/29
 */
public class VarIntByteBufferTest {
    public static void main(String[] args) {
        int a = Integer.MIN_VALUE;
        int b = Integer.MIN_VALUE / 2;
        int c = 0;
        int d = Integer.MAX_VALUE / 2;
        int e = Integer.MAX_VALUE;
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        ByteBufferUtils.toWriteMode(byteBuffer);
        VarIntUtils.writeRawVarInt32(byteBuffer, a);
        VarIntUtils.writeRawVarInt32(byteBuffer, b);
        VarIntUtils.writeRawVarInt32(byteBuffer, c);
        VarIntUtils.writeRawVarInt32(byteBuffer, d);
        VarIntUtils.writeRawVarInt32(byteBuffer, e);

        ByteBufferUtils.toReadMode(byteBuffer);
        System.out.println(VarIntUtils.readRawVarInt32(byteBuffer));
        System.out.println(VarIntUtils.readRawVarInt32(byteBuffer));
        System.out.println(VarIntUtils.readRawVarInt32(byteBuffer));
        System.out.println(VarIntUtils.readRawVarInt32(byteBuffer));
        System.out.println(VarIntUtils.readRawVarInt32(byteBuffer));

        System.out.println("-------------------------------------------");

        long a1 = Long.MIN_VALUE;
        long b1 = Long.MIN_VALUE / 2;
        long c1 = 0;
        long d1 = Long.MAX_VALUE / 2;
        long e1 = Long.MAX_VALUE;
        ByteBuffer byteBuffer1 = ByteBuffer.allocate(256);
        ByteBufferUtils.toWriteMode(byteBuffer1);
        VarIntUtils.writeRawVarInt64(byteBuffer1, a1);
        VarIntUtils.writeRawVarInt64(byteBuffer1, b1);
        VarIntUtils.writeRawVarInt64(byteBuffer1, c1);
        VarIntUtils.writeRawVarInt64(byteBuffer1, d1);
        VarIntUtils.writeRawVarInt64(byteBuffer1, e1);

        ByteBufferUtils.toReadMode(byteBuffer1);
        System.out.println(VarIntUtils.readRawVarInt64(byteBuffer1));
        System.out.println(VarIntUtils.readRawVarInt64(byteBuffer1));
        System.out.println(VarIntUtils.readRawVarInt64(byteBuffer1));
        System.out.println(VarIntUtils.readRawVarInt64(byteBuffer1));
        System.out.println(VarIntUtils.readRawVarInt64(byteBuffer1));
    }
}
