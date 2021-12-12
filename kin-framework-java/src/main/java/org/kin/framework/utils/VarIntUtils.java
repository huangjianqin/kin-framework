package org.kin.framework.utils;

import org.kin.framework.io.ByteBufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 变长整形工具类
 * <p>
 * 值越小的数字, 占用的字节越少
 * 通过减少表示数字的字节数, 从而进行数据压缩
 * 每个字节的最高位都是一个标志:
 * 如果是1: 表示后续的字节也是该数字的一部分
 * 如果是0: 表示这是最后一个字节, 剩余7位都是用来表示数字
 * <p>
 * 小整数对应的ZigZag码字短，大整数对应的ZigZag码字长
 *
 * @author huangjianqin
 * @date 2021/7/31
 */
public final class VarIntUtils {
    private VarIntUtils() {
    }

    public static int readRawVarInt32(ByteBuffer byteBuffer) {
        return readRawVarInt32(byteBuffer, true);
    }

    public static int readRawVarInt32(ByteBuffer byteBuffer, boolean zigzag) {
        return readRawVarInt32(new ByteBufferInput(byteBuffer), zigzag);
    }

    public static int readRawVarInt32(InputStream inputStream) {
        return readRawVarInt32(inputStream, true);
    }

    public static int readRawVarInt32(InputStream inputStream, boolean zigzag) {
        return readRawVarInt32(new StreamInput(inputStream), zigzag);
    }

    public static void writeRawVarInt32(ByteBuffer byteBuffer, int value) {
        writeRawVarInt32(byteBuffer, value, true);
    }

    public static void writeRawVarInt32(ByteBuffer byteBuffer, int value, boolean zigzag) {
        writeRawVarInt32(new ByteBufferOutput(byteBuffer), value, zigzag);
    }

    public static void writeRawVarInt32(OutputStream outputStream, int value) {
        writeRawVarInt32(outputStream, value, true);
    }

    public static void writeRawVarInt32(OutputStream outputStream, int value, boolean zigzag) {
        writeRawVarInt32(new StreamOutput(outputStream), value, zigzag);
    }

    public static long readRawVarLong64(ByteBuffer byteBuffer) {
        return readRawVarLong64(byteBuffer, true);
    }

    public static long readRawVarLong64(ByteBuffer byteBuffer, boolean zigzag) {
        return readRawVarLong64(new ByteBufferInput(byteBuffer), zigzag);
    }

    public static long readRawVarLong64(InputStream inputStream) {
        return readRawVarLong64(inputStream, true);
    }

    public static long readRawVarLong64(InputStream inputStream, boolean zigzag) {
        return readRawVarLong64(new StreamInput(inputStream), zigzag);
    }

    public static void writeRawVarLong64(ByteBuffer byteBuffer, long value) {
        writeRawVarLong64(byteBuffer, value, true);
    }

    public static void writeRawVarLong64(ByteBuffer byteBuffer, long value, boolean zigzag) {
        writeRawVarLong64(new ByteBufferOutput(byteBuffer), value, zigzag);
    }

    public static void writeRawVarLong64(OutputStream outputStream, long value) {
        writeRawVarLong64(outputStream, value, true);
    }

    public static void writeRawVarLong64(OutputStream outputStream, long value, boolean zigzag) {
        writeRawVarLong64(new StreamOutput(outputStream), value, zigzag);
    }

    //------------------------------------------var int/long reader 算法来自于protocolbuf------------------------------------------
    public static int readRawVarInt32(VarIntInput input) {
        return readRawVarInt32(input, true);
    }

    public static int readRawVarInt32(VarIntInput input, boolean zigzag) {
        int rawVarInt32 = _readRawVarInt32(input);
        if (zigzag) {
            return decodeZigZag32(rawVarInt32);
        } else {
            return rawVarInt32;
        }
    }

    /**
     * Decode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 32-bit integer, stored in a signed int because Java has no explicit
     *          unsigned support.
     * @return A signed 32-bit integer.
     */
    public static int decodeZigZag32(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * read 变长 32位int
     */
    private static int _readRawVarInt32(VarIntInput input) {
        if (!input.readerIndexSupported()) {
            return (int) readRawVarInt64SlowPath(input);
        }
        fastpath:
        {
            int readerIndex = input.readerIndex();
            if (input.readableBytes() <= 0) {
                break fastpath;
            }

            int x;
            if ((x = input.readByte()) >= 0) {
                return x;
            } else if (input.readableBytes() < 9) {
                //reset reader index
                input.readerIndex(readerIndex);
                // Will throw malformedVarint()
                break fastpath;
            } else if ((x ^= (input.readByte() << 7)) < 0) {
                x ^= (~0 << 7);
            } else if ((x ^= (input.readByte() << 14)) >= 0) {
                x ^= (~0 << 7) ^ (~0 << 14);
            } else if ((x ^= (input.readByte() << 21)) < 0) {
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
            } else {
                int y = input.readByte();
                x ^= y << 28;
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
                if (y < 0
                        && input.readByte() < 0
                        && input.readByte() < 0
                        && input.readByte() < 0
                        && input.readByte() < 0
                        && input.readByte() < 0) {
                    //reset reader index
                    input.readerIndex(readerIndex);
                    // Will throw malformedVarint()
                    break fastpath;
                }
            }
            return x;
        }
        return (int) readRawVarInt64SlowPath(input);
    }

    private static long readRawVarInt64SlowPath(VarIntInput input) {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            final byte b = readRawByte(input);
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("encountered a malformed var int");
    }

    public static long readRawVarLong64(VarIntInput input) {
        return readRawVarLong64(input, true);
    }

    public static long readRawVarLong64(VarIntInput input, boolean zigzag) {
        long rawVarLong64 = _readRawVarLong64(input);
        if (zigzag) {
            return decodeZigZag64(rawVarLong64);
        } else {
            return rawVarLong64;
        }
    }

    /**
     * Decode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n An unsigned 64-bit integer, stored in a signed int because Java has no explicit
     *          unsigned support.
     * @return A signed 64-bit integer.
     */
    public static long decodeZigZag64(long n) {
        return (n >>> 1) ^ -(n & 1);
    }

    /**
     * read 变长 64位long
     */
    private static long _readRawVarLong64(VarIntInput input) {
        if (!input.readerIndexSupported()) {
            return readRawVarInt64SlowPath(input);
        }

        // Implementation notes:
        //
        // Optimized for one-byte values, expected to be common.
        // The particular code below was selected from various candidates
        // empirically, by winning VarintBenchmark.
        //
        // Sign extension of (signed) Java bytes is usually a nuisance, but
        // we exploit it here to more easily obtain the sign of bytes read.
        // Instead of cleaning up the sign extension bits by masking eagerly,
        // we delay until we find the final (positive) byte, when we clear all
        // accumulated bits with one xor.  We depend on javac to constant fold.
        fastpath:
        {
            int readerIndex = input.readerIndex();
            if (input.readableBytes() <= 0) {
                break fastpath;
            }

            long x;
            int y;
            if ((y = input.readByte()) >= 0) {
                return y;
            } else if (input.readableBytes() < 9) {
                //reset reader index
                input.readerIndex(readerIndex);
                // Will throw malformedVarint()
                break fastpath;
            } else if ((y ^= (input.readByte() << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (input.readByte() << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (input.readByte() << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) input.readByte() << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) input.readByte() << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) input.readByte() << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) input.readByte() << 49)) < 0L) {
                x ^=
                        (~0L << 7)
                                ^ (~0L << 14)
                                ^ (~0L << 21)
                                ^ (~0L << 28)
                                ^ (~0L << 35)
                                ^ (~0L << 42)
                                ^ (~0L << 49);
            } else {
                x ^= ((long) input.readByte() << 56);
                x ^=
                        (~0L << 7)
                                ^ (~0L << 14)
                                ^ (~0L << 21)
                                ^ (~0L << 28)
                                ^ (~0L << 35)
                                ^ (~0L << 42)
                                ^ (~0L << 49)
                                ^ (~0L << 56);
                if (x < 0L) {
                    if (input.readByte() < 0L) {
                        //reset reader index
                        input.readerIndex(readerIndex);
                        // Will throw malformedVarint()
                        break fastpath;
                    }
                }
            }
            return x;
        }
        return readRawVarInt64SlowPath(input);
    }

    private static byte readRawByte(VarIntInput input) {
        if (input.readableBytes() <= 0) {
            throw new IllegalArgumentException("unexpect readable bytes");
        }
        return input.readByte();
    }

    //------------------------------------------var int/long writer 算法来自于protocolbuf------------------------------------------

    public static void writeRawVarInt32(VarIntOutput output, int value) {
        writeRawVarInt32(output, value, true);
    }

    public static void writeRawVarInt32(VarIntOutput output, int value, boolean zigzag) {
        if (zigzag) {
            value = encodeZigZag32(value);
        }
        _writeRawVarInt32(output, value);
    }

    /**
     * Encode a ZigZag-encoded 32-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 32-bit integer.
     * @return An unsigned 32-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     */
    public static int encodeZigZag32(int n) {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 31);
    }

    private static void _writeRawVarInt32(VarIntOutput output, int value) {
        //最大可写字节数
        int writableBytes = output.writableBytes();
        if (writableBytes < 5) {
            //不足5个byte, 则无法操作
            throw new IllegalArgumentException("output max writable bytes is less than 5 byte");
        }

        while (true) {
            if ((value & ~0x7F) == 0) {
                output.writeByte(value);
                return;
            } else {
                output.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }

    public static void writeRawVarLong64(VarIntOutput output, long value) {
        writeRawVarLong64(output, value, true);
    }

    public static void writeRawVarLong64(VarIntOutput output, long value, boolean zigzag) {
        if (zigzag) {
            value = encodeZigZag64(value);
        }
        _writRawVarLong64(output, value);
    }

    /**
     * Encode a ZigZag-encoded 64-bit value. ZigZag encodes signed integers into values that can be
     * efficiently encoded with varint. (Otherwise, negative values must be sign-extended to 64 bits
     * to be varint encoded, thus always taking 10 bytes on the wire.)
     *
     * @param n A signed 64-bit integer.
     * @return An unsigned 64-bit integer, stored in a signed int because Java has no explicit
     * unsigned support.
     */
    public static long encodeZigZag64(long n) {
        // Note:  the right-shift must be arithmetic
        return (n << 1) ^ (n >> 63);
    }

    private static void _writRawVarLong64(VarIntOutput output, long value) {
        //最大可写字节数
        int writableBytes = output.writableBytes();
        if (writableBytes < 9) {
            //不足9个byte, 则无法操作
            throw new IllegalArgumentException("output max writable bytes is less than 9 byte");
        }

        while (true) {
            if ((value & ~0x7FL) == 0) {
                output.writeByte((int) value);
                return;
            } else {
                output.writeByte((int) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------------

    /**
     * 计算32位变长正数占用的bytes数
     * Compute the number of bytes that would be needed to encode a varInt. {@code value} is treated as unsigned, so it
     * won't be sign-extended if negative.
     */
    public static int computeRawVarInt32Size(int value) {
        if ((value & (~0 << 7)) == 0) {
            return 1;
        }
        if ((value & (~0 << 14)) == 0) {
            return 2;
        }
        if ((value & (~0 << 21)) == 0) {
            return 3;
        }
        if ((value & (~0 << 28)) == 0) {
            return 4;
        }
        return 5;
    }

    /**
     * 计算64位变长正数占用的bytes数
     */
    public static int computeRawVarInt64Size(long value) {
        // Handle two popular special cases up front ...
        if ((value & (~0L << 7)) == 0L) {
            return 1;
        }
        if (value < 0L) {
            return 10;
        }
        // ... leaving us with 8 remaining, which we can divide and conquer
        int n = 2;
        if ((value & (~0L << 35)) != 0L) {
            n += 4;
            value >>>= 28;
        }
        if ((value & (~0L << 21)) != 0L) {
            n += 2;
            value >>>= 14;
        }
        if ((value & (~0L << 14)) != 0L) {
            n += 1;
        }
        return n;
    }

    //----------------------------------------------------------------------------------------------------------------

    /**
     * 基于{@link ByteBuffer}的{@link VarIntInput}实现
     */
    private static class ByteBufferInput implements VarIntInput {
        private final ByteBuffer byteBuffer;

        ByteBufferInput(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public byte readByte() {
            return byteBuffer.get();
        }

        @Override
        public int readerIndex() {
            return byteBuffer.position();
        }

        @Override
        public void readerIndex(int readerIndex) {
            byteBuffer.position(readerIndex);
        }

        @Override
        public boolean readerIndexSupported() {
            return true;
        }

        @Override
        public int readableBytes() {
            return ByteBufferUtils.getReadableBytes(byteBuffer);
        }
    }

    /**
     * 基于{@link ByteBuffer}的{@link VarIntOutput}实现
     */
    private static class ByteBufferOutput implements VarIntOutput {
        private final ByteBuffer byteBuffer;

        public ByteBufferOutput(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }

        @Override
        public void writeByte(int value) {
            byteBuffer.put((byte) value);
        }

        @Override
        public int writableBytes() {
            return ByteBufferUtils.getMaxWritableBytes(byteBuffer);
        }
    }

    /**
     * 基于{@link InputStream}的{@link VarIntInput}实现
     */
    private static class StreamInput implements VarIntInput {
        private final InputStream inputStream;

        public StreamInput(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public byte readByte() {
            try {
                return (byte) inputStream.read();
            } catch (IOException e) {
                ExceptionUtils.throwExt(e);
            }

            //理论上不会到这里
            return 0;
        }

        @Override
        public int readableBytes() {
            try {
                return inputStream.available();
            } catch (IOException e) {
                ExceptionUtils.throwExt(e);
            }

            //理论上不会到这里
            return 0;
        }
    }

    /**
     * 基于{@link OutputStream}的{@link VarIntOutput}实现
     */
    private static class StreamOutput implements VarIntOutput {
        private final OutputStream outputStream;

        public StreamOutput(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void writeByte(int value) {
            try {
                outputStream.write(value);
            } catch (IOException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        @Override
        public int writableBytes() {
            //认为是可无限写入
            return Integer.MAX_VALUE;
        }
    }
}