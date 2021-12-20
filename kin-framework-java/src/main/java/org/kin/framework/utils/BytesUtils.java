package org.kin.framework.utils;

import org.kin.framework.io.Input;
import org.kin.framework.io.Output;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 字节数组工具类
 *
 * @author huangjianqin
 * @date 2021/10/23
 */
public final class BytesUtils {
    private BytesUtils() {
    }

    public static byte getByte(byte[] memory, int index) {
        return memory[index];
    }

    public static short getShort(byte[] memory) {
        return getShort(memory, 0);
    }

    public static short getShort(byte[] memory, int index) {
        return (short) (memory[index] << 8 | memory[index + 1] & 0xFF);
    }

    public static short getShortLE(byte[] memory, int index) {
        return (short) (memory[index] & 0xff | memory[index + 1] << 8);
    }

    public static int getUnsignedMedium(byte[] memory) {
        return getUnsignedMedium(memory, 0);
    }

    public static int getUnsignedMedium(byte[] memory, int index) {
        return (memory[index] & 0xff) << 16 | (memory[index + 1] & 0xff) << 8 | memory[index + 2] & 0xff;
    }

    public static int getUnsignedMediumLE(byte[] memory, int index) {
        return memory[index] & 0xff | (memory[index + 1] & 0xff) << 8 | (memory[index + 2] & 0xff) << 16;
    }

    public static int getInt(byte[] memory) {
        return getInt(memory, 0);
    }

    public static int getInt(byte[] memory, int index) {
        return (memory[index] & 0xff) << 24 | (memory[index + 1] & 0xff) << 16 | (memory[index + 2] & 0xff) << 8
                | memory[index + 3] & 0xff;
    }

    public static int getIntLE(byte[] memory, int index) {
        return memory[index] & 0xff | (memory[index + 1] & 0xff) << 8 | (memory[index + 2] & 0xff) << 16
                | (memory[index + 3] & 0xff) << 24;
    }

    public static long getLong(byte[] memory) {
        return getLong(memory, 0);
    }

    public static long getLong(byte[] memory, int index) {
        return ((long) memory[index] & 0xff) << 56 | ((long) memory[index + 1] & 0xff) << 48
                | ((long) memory[index + 2] & 0xff) << 40 | ((long) memory[index + 3] & 0xff) << 32
                | ((long) memory[index + 4] & 0xff) << 24 | ((long) memory[index + 5] & 0xff) << 16
                | ((long) memory[index + 6] & 0xff) << 8 | (long) memory[index + 7] & 0xff;
    }

    public static long getLongLE(byte[] memory, int index) {
        return (long) memory[index] & 0xff | ((long) memory[index + 1] & 0xff) << 8
                | ((long) memory[index + 2] & 0xff) << 16 | ((long) memory[index + 3] & 0xff) << 24
                | ((long) memory[index + 4] & 0xff) << 32 | ((long) memory[index + 5] & 0xff) << 40
                | ((long) memory[index + 6] & 0xff) << 48 | ((long) memory[index + 7] & 0xff) << 56;
    }

    public static void setByte(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
    }

    public static void setShort(byte[] memory, int value) {
        setShort(memory, 0, value);
    }

    public static void setShort(byte[] memory, int index, int value) {
        memory[index] = (byte) (value >>> 8);
        memory[index + 1] = (byte) value;
    }

    public static void setShortLE(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
    }

    public static void setMedium(byte[] memory, int value) {
        setMedium(memory, 0, value);
    }

    public static void setMedium(byte[] memory, int index, int value) {
        memory[index] = (byte) (value >>> 16);
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) value;
    }

    public static void setMediumLE(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) (value >>> 16);
    }

    public static void setInt(byte[] memory, int value) {
        setInt(memory, 0, value);
    }

    public static void setInt(byte[] memory, int index, int value) {
        memory[index] = (byte) (value >>> 24);
        memory[index + 1] = (byte) (value >>> 16);
        memory[index + 2] = (byte) (value >>> 8);
        memory[index + 3] = (byte) value;
    }

    public static void setIntLE(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) (value >>> 16);
        memory[index + 3] = (byte) (value >>> 24);
    }

    public static void setLong(byte[] memory, long value) {
        setLong(memory, 0, value);
    }

    public static void setLong(byte[] memory, int index, long value) {
        memory[index] = (byte) (value >>> 56);
        memory[index + 1] = (byte) (value >>> 48);
        memory[index + 2] = (byte) (value >>> 40);
        memory[index + 3] = (byte) (value >>> 32);
        memory[index + 4] = (byte) (value >>> 24);
        memory[index + 5] = (byte) (value >>> 16);
        memory[index + 6] = (byte) (value >>> 8);
        memory[index + 7] = (byte) value;
    }

    public static void setLongLE(byte[] memory, int index, long value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) (value >>> 16);
        memory[index + 3] = (byte) (value >>> 24);
        memory[index + 4] = (byte) (value >>> 32);
        memory[index + 5] = (byte) (value >>> 40);
        memory[index + 6] = (byte) (value >>> 48);
        memory[index + 7] = (byte) (value >>> 56);
    }

    /**
     * Read a 32-bit little-endian integer from input stream.
     */
    public static int readInt32LE(InputStream inputStream) {
        try {
            return readInt32LE(inputStream.read(), inputStream.read(),
                    inputStream.read(), inputStream.read());
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        //理论上不会到这里
        return 0;
    }

    /**
     * Read a 32-bit little-endian integer from input.
     */
    public static int readInt32LE(Input input) {
        return readInt32LE(input.readByte(), input.readByte(),
                input.readByte(), input.readByte());
    }

    /**
     * Read a 32-bit little-endian integer.
     */
    private static int readInt32LE(int b1, int b2, int b3, int b4) {
        return ((b1 & 0xff)) |
                ((b2 & 0xff) << 8) |
                ((b3 & 0xff) << 16) |
                ((b4 & 0xff) << 24);
    }

    /**
     * Read a 32-bit little-endian integer from the internal buffer.
     */
    public static int readInt32LE(ByteBuffer byteBuffer) {
        return readInt32LE(byteBuffer.get(), byteBuffer.get(),
                byteBuffer.get(), byteBuffer.get());
    }

    /**
     * Read a float from input stream.
     */
    public static float readFloatLE(InputStream inputStream) {
        return Float.intBitsToFloat(readInt32LE(inputStream));
    }

    /**
     * Read a float from internal buffer.
     */
    public static float readFloatLE(ByteBuffer byteBuffer) {
        return Float.intBitsToFloat(readInt32LE(byteBuffer));
    }

    /**
     * Read a float from input.
     */
    public static float readFloatLE(Input input) {
        return Float.intBitsToFloat(readInt32LE(input));
    }

    /**
     * write a 32-bit little-endian integer to output stream.
     */
    public static void writeInt32LE(OutputStream outputStream, int value) {
        try {
            outputStream.write((byte) value);
            outputStream.write((byte) (value >>> 8));
            outputStream.write((byte) (value >>> 16));
            outputStream.write((byte) (value >>> 24));
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * write a 32-bit little-endian integer to output.
     */
    public static void writeInt32LE(Output output, int value) {
        output.writeByte((byte) value);
        output.writeByte((byte) (value >>> 8));
        output.writeByte((byte) (value >>> 16));
        output.writeByte((byte) (value >>> 24));
    }

    /**
     * write a 32-bit little-endian integer to the internal buffer.
     */
    public static void writeInt32LE(ByteBuffer byteBuffer, int value) {
        byteBuffer.put((byte) value);
        byteBuffer.put((byte) (value >>> 8));
        byteBuffer.put((byte) (value >>> 16));
        byteBuffer.put((byte) (value >>> 24));
    }

    /**
     * write a float to output stream.
     */
    public static void writeFloatLE(OutputStream outputStream, float value) {
        writeInt32LE(outputStream, Float.floatToRawIntBits(value));
    }

    /**
     * write a float to the internal buffer.
     */
    public static void writeFloatLE(ByteBuffer byteBuffer, float value) {
        writeInt32LE(byteBuffer, Float.floatToRawIntBits(value));
    }

    /**
     * write a float to the output.
     */
    public static void writeFloatLE(Output output, float value) {
        writeInt32LE(output, Float.floatToRawIntBits(value));
    }

    /**
     * Read a 64-bit little-endian integer from input stream.
     */
    public static long readInt64LE(InputStream inputStream) {
        try {
            return readInt64LE(inputStream.read(), inputStream.read(),
                    inputStream.read(), inputStream.read(),
                    inputStream.read(), inputStream.read(),
                    inputStream.read(), inputStream.read());
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        //理论上不会到这里
        return 0;
    }

    /**
     * Read a 64-bit little-endian integer from input.
     */
    public static long readInt64LE(Input input) {
        return readInt64LE(input.readByte(), input.readByte(),
                input.readByte(), input.readByte(),
                input.readByte(), input.readByte(),
                input.readByte(), input.readByte());
    }

    /**
     * Read a 64-bit little-endian integer.
     */
    private static long readInt64LE(long b1, long b2, long b3, long b4,
                                    long b5, long b6, long b7, long b8) {
        return ((b1 & 0xff)) |
                ((b2 & 0xff) << 8) |
                ((b3 & 0xff) << 16) |
                ((b4 & 0xff) << 24) |
                ((b5 & 0xff) << 32) |
                ((b6 & 0xff) << 40) |
                ((b7 & 0xff) << 48) |
                ((b8 & 0xff) << 56);
    }

    /**
     * Read a 64-bit little-endian integer from the internal byte buffer.
     */
    public static long readInt64LE(ByteBuffer byteBuffer) {
        return readInt64LE(byteBuffer.get(), byteBuffer.get(),
                byteBuffer.get(), byteBuffer.get(),
                byteBuffer.get(), byteBuffer.get(),
                byteBuffer.get(), byteBuffer.get());
    }

    /**
     * Read a double from input stream.
     */
    public static double readDoubleLE(InputStream inputStream) {
        return Double.longBitsToDouble(readInt64LE(inputStream));
    }

    /**
     * Read a double from byte buffer.
     */
    public static double readDoubleLE(ByteBuffer byteBuffer) {
        return Double.longBitsToDouble(readInt64LE(byteBuffer));
    }

    /**
     * Read a double from input.
     */
    public static double readDoubleLE(Input input) {
        return Double.longBitsToDouble(readInt64LE(input));
    }

    /**
     * write a 64-bit little-endian integer to output stream.
     */
    public static void writeInt64LE(OutputStream outputStream, long value) {
        try {
            outputStream.write((byte) value);
            outputStream.write((byte) (value >>> 8));
            outputStream.write((byte) (value >>> 16));
            outputStream.write((byte) (value >>> 24));
            outputStream.write((byte) (value >>> 32));
            outputStream.write((byte) (value >>> 40));
            outputStream.write((byte) (value >>> 48));
            outputStream.write((byte) (value >>> 56));
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * write a 64-bit little-endian integer to output.
     */
    public static void writeInt64LE(Output output, long value) {
        output.writeByte((byte) value);
        output.writeByte((byte) (value >>> 8));
        output.writeByte((byte) (value >>> 16));
        output.writeByte((byte) (value >>> 24));
        output.writeByte((byte) (value >>> 32));
        output.writeByte((byte) (value >>> 40));
        output.writeByte((byte) (value >>> 48));
        output.writeByte((byte) (value >>> 56));
    }

    /**
     * write a 64-bit little-endian integer to the internal buffer.
     */
    public static void writeInt64LE(ByteBuffer byteBuffer, long value) {
        byteBuffer.put((byte) value);
        byteBuffer.put((byte) (value >>> 8));
        byteBuffer.put((byte) (value >>> 16));
        byteBuffer.put((byte) (value >>> 24));
        byteBuffer.put((byte) (value >>> 32));
        byteBuffer.put((byte) (value >>> 40));
        byteBuffer.put((byte) (value >>> 48));
        byteBuffer.put((byte) (value >>> 56));
    }

    /**
     * write a double to output stream.
     */
    public static void writeDoubleLE(OutputStream outputStream, double value) {
        writeInt64LE(outputStream, Double.doubleToRawLongBits(value));
    }

    /**
     * write a double to the internal buffer.
     */
    public static void writeDoubleLE(ByteBuffer byteBuffer, double value) {
        writeInt64LE(byteBuffer, Double.doubleToRawLongBits(value));
    }

    /**
     * write a double to output.
     */
    public static void writeDoubleLE(Output output, double value) {
        writeInt64LE(output, Double.doubleToRawLongBits(value));
    }
}
