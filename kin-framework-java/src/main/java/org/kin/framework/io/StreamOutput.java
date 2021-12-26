package org.kin.framework.io;

import org.kin.framework.utils.ExceptionUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 基于{@link OutputStream}的{@link Output}实现
 *
 * @author huangjianqin
 * @date 2021/12/13
 */
public class StreamOutput implements Output {
    private final OutputStream outputStream;

    public StreamOutput(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void writeByte(int value) {
        try {
            outputStream.write(value);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    @Override
    public void writeBytes(byte[] value, int startIdx, int len) {
        try {
            outputStream.write(value, startIdx, len);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    @Override
    public int writableBytes() {
        //认为是可无限写入
        return Integer.MAX_VALUE;
    }
}
