package org.kin.framework.io;

import org.kin.framework.utils.Symbols;

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

        UnsafeByteBufferInput input = new UnsafeByteBufferInput(byteBuffer);
        for (int i = 0; i < 64; i++) {
            System.out.print(input.readByte() + Symbols.COMMA);
        }
        System.out.println("");
    }
}
