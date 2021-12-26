package org.kin.framework.io;

import java.nio.ByteBuffer;

/**
 * 基于{@link ByteBuffer}的{@link Output}实现
 *
 * @author huangjianqin
 * @date 2021/12/13
 */
public class ByteBufferOutput implements Output {
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
