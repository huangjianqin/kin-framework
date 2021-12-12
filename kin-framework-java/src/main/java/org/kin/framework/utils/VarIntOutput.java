package org.kin.framework.utils;

/**
 * 变长整形output抽象
 *
 * @author huangjianqin
 * @date 2021/12/12
 */
public interface VarIntOutput {
    /** 写一个字节 */
    void writeByte(int value);

    /** 返回可写字节数 */
    int writableBytes();
}
