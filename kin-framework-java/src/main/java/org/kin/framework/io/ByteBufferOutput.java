package org.kin.framework.io;

import org.kin.framework.concurrent.FastThreadLocal;

import java.nio.ByteBuffer;

/**
 * 基于{@link ByteBuffer}的{@link Output}实现
 *
 * @author huangjianqin
 * @date 2021/12/13
 */
public class ByteBufferOutput implements Output {
    /** 为了减少创建{@link ByteBufferOutput}实例, 比如RPC序列化场景 */
    private static final FastThreadLocal<ByteBufferOutput> THREAD_LOCAL_BYTEBUFFER_OUTPUT = new FastThreadLocal<ByteBufferOutput>() {
        @Override
        protected ByteBufferOutput initialValue() {
            return new ByteBufferOutput(null);
        }
    };

    public static ByteBufferOutput current(ByteBuffer byteBuffer) {
        ByteBufferOutput byteBufferOutput = THREAD_LOCAL_BYTEBUFFER_OUTPUT.get();
        byteBufferOutput.byteBuffer = byteBuffer;
        return byteBufferOutput;
    }

    protected ByteBuffer byteBuffer;

    public ByteBufferOutput(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public void writeByte(int value) {
        byteBuffer = ByteBufferUtils.ensureWritableBytes(this.byteBuffer, 1);
        byteBuffer.put((byte) value);
    }

    @Override
    public void writeBytes(byte[] value, int startIdx, int len) {
        byteBuffer = ByteBufferUtils.ensureWritableBytes(this.byteBuffer, len);
        byteBuffer.put(value, startIdx, len);
    }

    @Override
    public int writableBytes() {
        return ByteBufferUtils.getWritableBytes(byteBuffer);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }
}
