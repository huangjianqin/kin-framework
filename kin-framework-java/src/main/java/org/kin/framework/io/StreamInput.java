package org.kin.framework.io;

import org.kin.framework.concurrent.FastThreadLocal;
import org.kin.framework.utils.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于{@link InputStream}的{@link Input}实现
 *
 * @author huangjianqin
 * @date 2021/12/13
 */
public class StreamInput implements Input {
    /** 为了减少创建{@link StreamInput}实例, 比如RPC序列化场景 */
    private static final FastThreadLocal<StreamInput> THREAD_LOCAL_STREAM_INPUT = new FastThreadLocal<StreamInput>() {
        @Override
        protected StreamInput initialValue() {
            return new StreamInput(null);
        }
    };

    public static StreamInput current(InputStream inputStream) {
        StreamInput streamInput = THREAD_LOCAL_STREAM_INPUT.get();
        streamInput.inputStream = inputStream;
        return streamInput;
    }

    private InputStream inputStream;

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