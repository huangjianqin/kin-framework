package org.kin.framework.utils;

import org.kin.framework.io.ByteBufferUtils;

import java.nio.ByteBuffer;

/**
 * 变长整形工具类
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
        return decodeZigZag32(_readRawVarInt32(byteBuffer));
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
    private static int decodeZigZag32(int n) {
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
        return (int) readRawVarint64SlowPath(byteBuffer);
    }

    private static long readRawVarint64SlowPath(ByteBuffer byteBuffer) {
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
        return decodeZigZag64(_readRawVarLong64(byteBuffer));
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
    private static long decodeZigZag64(long n) {
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
        return readRawVarint64SlowPath(byteBuffer);
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
        _writeRawVarInt32(byteBuffer, encodeZigZag32(value));
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
    private static int encodeZigZag32(int n) {
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
    public static void writeRawVarlong64(ByteBuffer byteBuffer, long value) {
        _writRawVarLong64(byteBuffer, encodeZigZag64(value));
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
    private static long encodeZigZag64(long n) {
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
}
