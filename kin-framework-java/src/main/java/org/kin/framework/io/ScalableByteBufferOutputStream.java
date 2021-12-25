package org.kin.framework.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 写入{@link ByteBuffer}的{@link OutputStream}实现
 * 支持自动扩容
 *
 * @author huangjianqin
 * @date 2021/11/28
 */
public final class ScalableByteBufferOutputStream extends OutputStream {
    /** sink */
    private ByteBuffer sink;

    public ScalableByteBufferOutputStream(int bufferSize) {
        this(bufferSize, false);
    }

    public ScalableByteBufferOutputStream(int bufferSize, boolean direct) {
        this(direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize));
    }

    public ScalableByteBufferOutputStream(ByteBuffer sink) {
        this.sink = sink;
    }

    @Override
    public void write(int b) throws IOException {
        if (!sink.hasRemaining()) {
            //double
            sink = ByteBufferUtils.ensureWritableBytes(sink, sink.capacity());
        }

        sink.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        if (sink.remaining() < length) {
            if (sink.capacity() * 2 < length) {
                //as need
                sink = ByteBufferUtils.ensureWritableBytes(sink, length);
            } else {
                //double
                sink = ByteBufferUtils.ensureWritableBytes(sink, sink.capacity());
            }
        }

        sink.put(bytes, offset, length);
    }

    public ByteBuffer getSink() {
        return sink;
    }
}