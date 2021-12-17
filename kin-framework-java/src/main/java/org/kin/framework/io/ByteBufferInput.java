package org.kin.framework.io;

import org.kin.framework.concurrent.FastThreadLocal;

import java.nio.ByteBuffer;

/**
 * 基于{@link ByteBuffer}的{@link Input}实现
 *
 * @author huangjianqin
 * @date 2021/12/13
 */
public class ByteBufferInput implements Input {
    /** 为了减少创建{@link ByteBufferInput}实例, 比如RPC序列化场景 */
    private static final FastThreadLocal<ByteBufferInput> THREAD_LOCAL_BYTEBUFFER_INPUT = new FastThreadLocal<ByteBufferInput>() {
        @Override
        protected ByteBufferInput initialValue() {
            return new ByteBufferInput(null);
        }
    };

    public static ByteBufferInput current(ByteBuffer byteBuffer) {
        ByteBufferInput byteBufferInput = THREAD_LOCAL_BYTEBUFFER_INPUT.get();
        byteBufferInput.byteBuffer = byteBuffer;
        return byteBufferInput;
    }

    private ByteBuffer byteBuffer;

    public ByteBufferInput(ByteBuffer byteBuffer) {
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
