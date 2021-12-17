package org.kin.framework.io;

/**
 * bytes writer
 *
 * @author huangjianqin
 * @date 2021/12/12
 */
public interface Output {
    /** 写入一个字节 */
    void writeByte(int value);

    /** 写入字节数组 */
    void writeBytes(byte[] value, int startIdx, int len);

    /** 写入字节数组 */
    default void writeBytes(byte[] value) {
        writeBytes(value, 0, value.length);
    }

    /** 返回可写字节数 */
    int writableBytes();
}
