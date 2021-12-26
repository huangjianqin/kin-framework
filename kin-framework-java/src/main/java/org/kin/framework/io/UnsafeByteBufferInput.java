package org.kin.framework.io;

import org.kin.framework.utils.UnsafeDirectBufferUtil;
import org.kin.framework.utils.UnsafeUtil;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2021/12/26
 */
public final class UnsafeByteBufferInput extends ByteBufferInput {
    /** buffer起始内存地址, 该buffer应该不可移动, 一般情况是off-heap */
    private final long memoryAddress;

    public UnsafeByteBufferInput(ByteBuffer byteBuffer) {
        super(byteBuffer);

        if (!byteBuffer.isDirect()) {
            throw new IllegalArgumentException("byteBuffer is not a off-heap buffer");
        }

        if (!UnsafeUtil.hasUnsafe()) {
            throw new IllegalStateException("jvm environment is not support unsafe operation");
        }

        this.memoryAddress = UnsafeUtil.addressOffset(byteBuffer);
    }

    /**
     * 获取buffer底层byte[]内存起始地址
     */
    private long address(int position) {
        return memoryAddress + position;
    }

    @Override
    public byte readByte() {
        int position = byteBuffer.position();
        byte ret = UnsafeDirectBufferUtil.getByte(address(position));
        //手动增加position
        byteBuffer.position(position + 1);
        return ret;
    }

    @Override
    public Input readBytes(byte[] dst, int dstIndex, int length) {
        if (Objects.isNull(dst)) {
            throw new IllegalArgumentException("dst is null");
        }
        if (dstIndex < 0) {
            throw new IndexOutOfBoundsException("dstIndex < 0");
        }
        if (readableBytes() < length) {
            throw new IndexOutOfBoundsException("length is greater than readableBytes");
        }

        int position = byteBuffer.position();
        UnsafeDirectBufferUtil.getBytes(address(position), dst, dstIndex, length);
        byteBuffer.position(position + length);
        return this;
    }
}
