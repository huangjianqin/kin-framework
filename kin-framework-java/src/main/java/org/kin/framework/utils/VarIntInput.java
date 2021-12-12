package org.kin.framework.utils;

/**
 * 变长整形input抽象
 *
 * @author huangjianqin
 * @date 2021/12/12
 */
public interface VarIntInput {
    /** 读取一个字节 */
    byte readByte();

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
    default void readerIndex(int readerIndex) {
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
