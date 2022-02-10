package org.kin.framework.io;

import org.kin.framework.utils.UnsafeDirectBufferUtil;
import org.kin.framework.utils.UnsafeUtil;

import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2021/12/26
 */
public final class UnsafeByteBufferOutput extends ByteBufferOutput {
    /** buffer起始内存地址, 该buffer应该不可移动, 一般情况是off-heap */
    private long memoryAddress;

    public UnsafeByteBufferOutput(ByteBuffer byteBuffer) {
        super(byteBuffer);

        if (!byteBuffer.isDirect()) {
            throw new IllegalArgumentException("byteBuffer is not a off-heap buffer");
        }

        if (!UnsafeUtil.hasUnsafe()) {
            throw new IllegalStateException("jvm environment is not support unsafe operation");
        }

        updateBufferAddress();
    }

    /**
     * 获取buffer底层byte[]内存起始地址
     */
    private long address(int position) {
        return memoryAddress + position;
    }

    /**
     * 更新buffer底层byte[]内存起始地址
     */
    private void updateBufferAddress() {
        memoryAddress = UnsafeUtil.addressOffset(byteBuffer);
    }

    /**
     * 保证bytebuffer有足够的写空间
     */
    private void ensureWritableBytes(int size) {
        ByteBuffer oldByteBuffer = byteBuffer;
        byteBuffer = ByteBufferUtils.ensureWritableBytes(oldByteBuffer, size);
        if (oldByteBuffer != byteBuffer) {
            //扩容
            updateBufferAddress();
        }
    }

    @Override
    public void writeByte(int value) {
        ensureWritableBytes(1);
        int position = byteBuffer.position();
        UnsafeDirectBufferUtil.setByte(address(position), value);
        byteBuffer.position(position + 1);
    }

    @Override
    public void writeBytes(byte[] value, int startIdx, int len) {
        ensureWritableBytes(len);
        int position = byteBuffer.position();
        UnsafeDirectBufferUtil.setBytes(address(position), value, startIdx, len);
        byteBuffer.position(position + len);
    }


}
