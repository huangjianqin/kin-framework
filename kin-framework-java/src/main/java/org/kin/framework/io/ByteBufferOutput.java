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
        if (!byteBuffer.hasRemaining()) {
            //double
            byteBuffer = ByteBufferUtils.expandCapacity(byteBuffer, byteBuffer.capacity() * 2);
        }
        byteBuffer.put((byte) value);
    }

    @Override
    public void writeBytes(byte[] value, int startIdx, int len) {
        byteBuffer = ByteBufferUtils.ensureWritableBytes(byteBuffer, len);
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
