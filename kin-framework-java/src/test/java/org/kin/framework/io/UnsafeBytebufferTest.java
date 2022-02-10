package org.kin.framework.io;

import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2021/12/26
 */
public class UnsafeBytebufferTest {
    public static void main(String[] args) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4);
        UnsafeByteBufferOutput output = new UnsafeByteBufferOutput(byteBuffer);
        for (int i = 0; i < 64; i++) {
            output.writeByte(i);
        }

        byteBuffer = output.getByteBuffer();
        ByteBufferUtils.toReadMode(byteBuffer);

        System.out.println("bytebuffer position:" + byteBuffer.position());
        System.out.println("bytebuffer limit:" + byteBuffer.limit());
        System.out.println("bytebuffer capacity:" + byteBuffer.capacity());

        UnsafeByteBufferInput input = new UnsafeByteBufferInput(byteBuffer);
        for (int i = 0; i < 64; i++) {
            System.out.print(input.readByte() + ",");
        }
        System.out.println("");
    }
}
