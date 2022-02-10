package org.kin.framework.io;

import org.kin.framework.utils.Maths;

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
            sink = ByteBufferUtils.expandCapacity(sink, sink.capacity() * 2);
        }

        sink.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        //当前可写最大字节数
        int nowWritableBytes = ByteBufferUtils.getWritableBytes(sink);
        if (nowWritableBytes < length) {
            //空间不足
            //两倍容量
            int doubleCapacity = sink.capacity() * 2;
            //新容量
            int newCapacity;
            if (doubleCapacity < length) {
                //> double, length round to power2
                newCapacity = Maths.round2Power2(length);
            } else {
                //double
                newCapacity = doubleCapacity;
            }
            sink = ByteBufferUtils.expandCapacity(sink, newCapacity);
        }

        sink.put(bytes, offset, length);
    }

    public ByteBuffer getSink() {
        return sink;
    }
}