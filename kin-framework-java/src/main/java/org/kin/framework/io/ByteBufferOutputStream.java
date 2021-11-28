package org.kin.framework.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * 写入{@link ByteBuffer}的{@link OutputStream}实现
 * 但{@link ByteBuffer}只会触发{@link #flush()}, 使用者自定义刷入外部存储空间逻辑
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
        if (!sink.hasRemaining()) {
            flush();
        }

        sink.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        if (sink.remaining() < length) {
            flush();
        }

        sink.put(bytes, offset, length);
    }

    public ByteBuffer getSink() {
        return sink;
    }
}
