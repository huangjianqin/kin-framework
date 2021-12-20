package org.kin.framework.io;

/**
 * bytes reader
 *
 * @author huangjianqin
 * @date 2021/12/12
 */
public interface Input {
    /** 读取一个字节 */
    byte readByte();

    /** 往{@code dst}写入指定长度({@code dst.length})的字节数据, 同时会改变{@link Input}的read index */
    default Input readBytes(byte[] dst) {
        readBytes(dst, 0, dst.length);
        return this;
    }

    /**
     * 往{@code dst}写入指定长度({@code length})的字节数据, 同时会改变{@link Input}的read index
     *
     * @param dstIndex {@code dst}开始写入的index
     */
    Input readBytes(byte[] dst, int dstIndex, int length);

    /**
     * 获取当前read index
     *
     * @return 当前read index
     */
    default int readerIndex() {
        //默认不支持
        throw new UnsupportedOperationException();
    }

    /**
     * 设置read index
     */
    default Input readerIndex(int readerIndex) {
        //默认不支持
        throw new UnsupportedOperationException();
    }

    /**
     * 是否支持{@link #readerIndex()}操作, 默认不支持
     * 如果支持{@link #readerIndex()}操作, 变长整形read操作效率更高
     */
    default boolean readerIndexSupported() {
        return false;
    }

    /** 返回可读取字节数 */
    int readableBytes();
}
