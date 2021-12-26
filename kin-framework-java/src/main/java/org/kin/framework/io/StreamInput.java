package org.kin.framework.io;

import org.kin.framework.utils.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * 基于{@link InputStream}的{@link Input}实现
 *
 * @author huangjianqin
 * @date 2021/12/13
 */
public class StreamInput implements Input {
    private final InputStream inputStream;

    public StreamInput(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public byte readByte() {
        try {
            return (byte) inputStream.read();
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        //理论上不会到这里
        return 0;
    }

    @Override
    public Input readBytes(byte[] dst, int dstIndex, int length) {
        if (Objects.isNull(dst)) {
            throw new IllegalArgumentException("dst is null");
        }
        if (dstIndex < 0) {
            throw new IndexOutOfBoundsException("dstIndex < 0");
        }
        if (readableBytes() < length) {
            throw new IndexOutOfBoundsException("length is greater than readableBytes");
        }
        try {
            inputStream.read(dst, dstIndex, length);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
        return this;
    }

    @Override
    public int readableBytes() {
        try {
            return inputStream.available();
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        //理论上不会到这里
        return 0;
    }
}
