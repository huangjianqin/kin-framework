package org.kin.framework.io;

import org.kin.framework.concurrent.FastThreadLocal;
import org.kin.framework.utils.UnsafeDirectBufferUtil;
import org.kin.framework.utils.UnsafeUtil;

import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2021/12/26
 */
public final class UnsafeByteBufferOutput extends ByteBufferOutput {
    /** 为了减少创建{@link UnsafeByteBufferOutput}实例, 比如RPC序列化场景 */
    private static final FastThreadLocal<UnsafeByteBufferOutput> THREAD_LOCAL_BYTEBUFFER_OUTPUT = new FastThreadLocal<UnsafeByteBufferOutput>() {
        @Override
        protected UnsafeByteBufferOutput initialValue() {
            return new UnsafeByteBufferOutput(null);
        }
    };

    public static UnsafeByteBufferOutput current(ByteBuffer byteBuffer) {
        UnsafeByteBufferOutput byteBufferOutput = THREAD_LOCAL_BYTEBUFFER_OUTPUT.get();
        byteBufferOutput.byteBuffer = byteBuffer;
        return byteBufferOutput;
    }

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
        ByteBuffer oldBBuffer = byteBuffer;
        byteBuffer = ByteBufferUtils.ensureWritableBytes(byteBuffer, size);
        if (oldBBuffer != byteBuffer) {
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
