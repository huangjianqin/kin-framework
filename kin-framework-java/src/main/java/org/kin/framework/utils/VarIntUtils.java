package org.kin.framework.utils;

import org.kin.framework.io.ByteBufferUtils;

import java.nio.ByteBuffer;

/**
 * 变长整形工具类
 * <p>
 * 值越小的数字, 占用的字节越少
 * 通过减少表示数字的字节数, 从而进行数据压缩
 * 每个字节的最高位都是一个标志:
 * 如果是1: 表示后续的字节也是该数字的一部分
 * 如果是0: 表示这是最后一个字节, 剩余7位都是用来表示数字
 *
 * @author huangjianqin
 * @date 2021/7/31
 */
public final class VarIntUtils {
    private VarIntUtils() {
    }

    //------------------------------------------var int/long reader 算法来自于protocolbuf------------------------------------------

    /**
     * 必须保证{@param byteBuffer}是可读状态
     *
     * @see ByteBufferUtils#toReadMode(ByteBuffer)
     */
    public static int readRawVarInt32(ByteBuffer byteBuffer) {
        return readRawVarInt32(byteBuffer, true);
    }

    /**
     * 必须保证{@param byteBuffer}是可读状态
     *
     * @see ByteBufferUtils#toReadMode(ByteBuffer)
     */
    public static int readRawVarInt32(ByteBuffer byteBuffer, boolean zigzag) {
        int rawVarInt32 = _readRawVarInt32(byteBuffer);
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
    private static int _readRawVarInt32(ByteBuffer byteBuffer) {
        fastpath:
        {
            int readerIndex = byteBuffer.position();
            if (ByteBufferUtils.getReadableBytes(byteBuffer) <= 0) {
                break fastpath;
            }

            int x;
            if ((x = byteBuffer.get()) >= 0) {
                return x;
            } else if (ByteBufferUtils.getReadableBytes(byteBuffer) < 9) {
                //reset reader index
                byteBuffer.position(readerIndex);
                break fastpath;
            } else if ((x ^= (byteBuffer.get() << 7)) < 0) {
                x ^= (~0 << 7);
            } else if ((x ^= (byteBuffer.get() << 14)) >= 0) {
                x ^= (~0 << 7) ^ (~0 << 14);
            } else if ((x ^= (byteBuffer.get() << 21)) < 0) {
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
            } else {
                int y = byteBuffer.get();
                x ^= y << 28;
                x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
                if (y < 0
                        && byteBuffer.get() < 0
                        && byteBuffer.get() < 0
                        && byteBuffer.get() < 0
                        && byteBuffer.get() < 0
                        && byteBuffer.get() < 0) {
                    //reset reader index
                    byteBuffer.position(readerIndex);
                    break fastpath; // Will throw malformedVarint()
                }
            }
            return x;
        }
        return (int) readRawVarInt64SlowPath(byteBuffer);
    }

    private static long readRawVarInt64SlowPath(ByteBuffer byteBuffer) {
        long result = 0;
        for (int shift = 0; shift < 64; shift += 7) {
            final byte b = readRawByte(byteBuffer);
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("encountered a malformed varint");
    }

    /**
     * 必须保证{@param byteBuffer}是可读状态
     *
     * @see ByteBufferUtils#toReadMode(ByteBuffer)
     */
    public static long readRawVarLong64(ByteBuffer byteBuffer) {
        return readRawVarLong64(byteBuffer, true);
    }

    /**
     * 必须保证{@param byteBuffer}是可读状态
     *
     * @see ByteBufferUtils#toReadMode(ByteBuffer)
     */
    public static long readRawVarLong64(ByteBuffer byteBuffer, boolean zigzag) {
        long rawVarLong64 = _readRawVarLong64(byteBuffer);
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
    private static long _readRawVarLong64(ByteBuffer byteBuffer) {
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
            int readerIndex = byteBuffer.position();

            if (ByteBufferUtils.getReadableBytes(byteBuffer) <= 0) {
                break fastpath;
            }

            long x;
            int y;
            if ((y = byteBuffer.get()) >= 0) {
                return y;
            } else if (ByteBufferUtils.getReadableBytes(byteBuffer) < 9) {
                //reset reader index
                byteBuffer.position(readerIndex);
                break fastpath;
            } else if ((y ^= (byteBuffer.get() << 7)) < 0) {
                x = y ^ (~0 << 7);
            } else if ((y ^= (byteBuffer.get() << 14)) >= 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14));
            } else if ((y ^= (byteBuffer.get() << 21)) < 0) {
                x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
            } else if ((x = y ^ ((long) byteBuffer.get() << 28)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
            } else if ((x ^= ((long) byteBuffer.get() << 35)) < 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
            } else if ((x ^= ((long) byteBuffer.get() << 42)) >= 0L) {
                x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
            } else if ((x ^= ((long) byteBuffer.get() << 49)) < 0L) {
                x ^=
                        (~0L << 7)
                                ^ (~0L << 14)
                                ^ (~0L << 21)
                                ^ (~0L << 28)
                                ^ (~0L << 35)
                                ^ (~0L << 42)
                                ^ (~0L << 49);
            } else {
                x ^= ((long) byteBuffer.get() << 56);
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
                    if (byteBuffer.get() < 0L) {
                        //reset reader index
                        byteBuffer.position(readerIndex);
                        break fastpath; // Will throw malformedVarint()
                    }
                }
            }
            return x;
        }
        return readRawVarInt64SlowPath(byteBuffer);
    }

    private static byte readRawByte(ByteBuffer byteBuffer) {
        if (ByteBufferUtils.getReadableBytes(byteBuffer) <= 0) {
            throw new IllegalStateException("While parsing a protocol, the input ended unexpectedly "
                    + "in the middle of a field.  This could mean either that the "
                    + "input has been truncated or that an embedded message "
                    + "misreported its own length.");
        }
        return byteBuffer.get();
    }

    //------------------------------------------var int/long writer 算法来自于protocolbuf------------------------------------------

    /**
     * 必须保证{@param byteBuffer}是可写状态
     *
     * @see ByteBufferUtils#toWriteMode(ByteBuffer)
     */
    public static void writeRawVarInt32(ByteBuffer byteBuffer, int value) {
        writeRawVarInt32(byteBuffer, value, true);
    }

    /**
     * 必须保证{@param byteBuffer}是可写状态
     *
     * @see ByteBufferUtils#toWriteMode(ByteBuffer)
     */
    public static void writeRawVarInt32(ByteBuffer byteBuffer, int value, boolean zigzag) {
        if (zigzag) {
            value = encodeZigZag32(value);
        }
        _writeRawVarInt32(byteBuffer, value);
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

    private static void _writeRawVarInt32(ByteBuffer byteBuffer, int value) {
        //bytebuffer 最大可写字节数
        int maxWritableBytes = ByteBufferUtils.getMaxWritableBytes(byteBuffer);
        if (maxWritableBytes < 5) {
            //不足5个byte, 则无法操作
            throw new IllegalArgumentException("byteBuffer max writable bytes is less than 5 byte");
        }

        while (true) {
            if ((value & ~0x7F) == 0) {
                byteBuffer.put((byte) value);
                return;
            } else {
                byteBuffer.put((byte) ((value & 0x7F) | 0x80));
                value >>>= 7;
            }
        }
    }

    /**
     * 必须保证{@param byteBuffer}是可写状态
     *
     * @see ByteBufferUtils#toWriteMode(ByteBuffer)
     */
    public static void writeRawVarLong64(ByteBuffer byteBuffer, long value) {
        writeRawVarLong64(byteBuffer, value, true);
    }

    /**
     * 必须保证{@param byteBuffer}是可写状态
     *
     * @see ByteBufferUtils#toWriteMode(ByteBuffer)
     */
    public static void writeRawVarLong64(ByteBuffer byteBuffer, long value, boolean zigzag) {
        if (zigzag) {
            value = encodeZigZag64(value);
        }
        _writRawVarLong64(byteBuffer, value);
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

    private static void _writRawVarLong64(ByteBuffer byteBuffer, long value) {
        //bytebuffer 最大可写字节数
        int maxWritableBytes = ByteBufferUtils.getMaxWritableBytes(byteBuffer);
        if (maxWritableBytes < 9) {
            //不足9个byte, 则无法操作
            throw new IllegalArgumentException("byteBuffer max writable bytes is less than 9 byte");
        }

        while (true) {
            if ((value & ~0x7FL) == 0) {
                byteBuffer.put((byte) value);
                return;
            } else {
                byteBuffer.put((byte) ((value & 0x7F) | 0x80));
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
}
