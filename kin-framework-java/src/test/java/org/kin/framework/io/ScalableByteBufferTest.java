package org.kin.framework.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2021/12/17
 */
public class ScalableByteBufferTest {
    public static void main(String[] args) {
        ScalableByteBuffer byteBuffer = new ScalableByteBuffer(8);
        for (int i = 0; i < 256; i++) {
            byteBuffer.writeByte(i);
        }
        System.out.println("-----------------------------------------------1");
        byteBuffer.toRead();

        System.out.println(byteBuffer);
        System.out.println(byteBuffer.writableBytes());
        System.out.println(byteBuffer.readableBytes());
        System.out.println(byteBuffer.readerIndex());

        List<Byte> read = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            read.add(byteBuffer.readByte());
        }

        System.out.println(read);
        System.out.println(byteBuffer.writableBytes());
        System.out.println(byteBuffer.readableBytes());
        System.out.println(byteBuffer.readerIndex());

        read.clear();
        for (int i = 0; i < 192; i++) {
            read.add(byteBuffer.readByte());
        }

        System.out.println(read);
        System.out.println(byteBuffer.writableBytes());
        System.out.println(byteBuffer.readableBytes());
        System.out.println(byteBuffer.readerIndex());

        System.out.println("-----------------------------------------------2");
        byteBuffer.toRead();
        read.clear();
        for (int i = 0; i < 128; i++) {
            read.add(byteBuffer.readByte());
        }

        System.out.println(read);
        System.out.println(byteBuffer.writableBytes());
        System.out.println(byteBuffer.readableBytes());
        System.out.println(byteBuffer.readerIndex());

        System.out.println("-----------------------------------------------3");
        byteBuffer.toWrite();
        System.out.println(byteBuffer);

        for (int i = 0; i < 128; i++) {
            byteBuffer.writeByte(i);
        }

        byteBuffer.writeByte(1);
        byteBuffer.writeByte(2);
        byteBuffer.writeByte(3);
        byteBuffer.toRead();

        System.out.println(byteBuffer);
        System.out.println(Arrays.toString(byteBuffer.toByteArray()));

        System.out.println("-----------------------------------------------5");
        byteBuffer.toWrite();
        byte[] bytes = new byte[]{1, 3, 5, 7, 9};
        byteBuffer.writeBytes(bytes);
        byteBuffer.writeBytes(bytes, 2, 2);
        byteBuffer.writeByte(-100);
        System.out.println(byteBuffer);

        System.out.println("-----------------------------------------------6");
        byteBuffer.toRead();
        System.out.println(Arrays.toString(byteBuffer.toByteArray()));
    }
}
