package org.kin.framework.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2021/11/28
 */
public final class ByteBufferInputStream extends InputStream {
    private ByteBuffer byteBuffer;

    public ByteBufferInputStream() {
    }

    public ByteBufferInputStream(int bufferSize) {
        this(ByteBuffer.allocate(bufferSize));
        this.byteBuffer.flip();
    }

    public ByteBufferInputStream(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        return this.byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public int read() throws IOException {
        return !this.byteBuffer.hasRemaining() ? -1 : this.byteBuffer.get();
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        if (length == 0) {
            return 0;
        } else {
            int count = Math.min(this.byteBuffer.remaining(), length);
            if (count == 0) {
                return -1;
            } else {
                this.byteBuffer.get(bytes, offset, count);
                return count;
            }
        }
    }

    @Override
    public int available() {
        return this.byteBuffer.remaining();
    }
}

