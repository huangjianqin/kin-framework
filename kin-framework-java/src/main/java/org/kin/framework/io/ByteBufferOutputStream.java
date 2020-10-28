package org.kin.framework.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 写 {@link java.nio.ByteBuffer} 的 output stream
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class ByteBufferOutputStream extends OutputStream {
    /** sink */
    private final ByteBuffer sink;

    public ByteBufferOutputStream(int bufferSize) {
        this(ByteBuffer.allocate(bufferSize));
    }

    public ByteBufferOutputStream(ByteBuffer sink) {
        this.sink = sink;
    }

    @Override
    public void write(int b) throws IOException {
        if (!this.sink.hasRemaining()) {
            this.flush();
        }

        this.sink.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        if (this.sink.remaining() < length) {
            this.flush();
        }

        this.sink.put(bytes, offset, length);
    }

    public ByteBuffer getSink() {
        return this.sink;
    }
}
