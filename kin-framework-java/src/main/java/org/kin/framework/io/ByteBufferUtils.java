package org.kin.framework.io;

import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2020/9/28
 */
public class ByteBufferUtils {
    /**
     * @return 是否是读模式
     */
    public static boolean isReadMode(ByteBuffer target) {
        return !isWriteMode(target);
    }

    /**
     * @return 是否是写模式
     */
    public static boolean isWriteMode(ByteBuffer target) {
        return target.position() != 0 || target.limit() == target.capacity();
    }

    /**
     * 切换为读模式
     * <p>
     * {@param target}本身要处于write mode, 不然存在数据丢失
     * 比如read了一点点数据, 再调用该方法, buffer本身只能再读之前读过的数据
     */
    public static void toReadMode(ByteBuffer target) {
        if (!isReadMode(target)) {
            //change read
            target.flip();
        }
    }

    /**
     * 切换为写模式
     */
    public static void toWriteMode(ByteBuffer target) {
        if (!isWriteMode(target)) {
            target.position(target.limit());
            target.limit(target.capacity());
        }
    }

    /**
     * 将{@param src}内容复制到{@param target}
     */
    public static void copy(ByteBuffer target, ByteBuffer src) {
        toWriteMode(target);
        toReadMode(src);

        target.put(src);
    }

    /**
     * 将{@param src}内容复制到{@param target}, 并清掉{@param src}内容
     */
    public static void copyAndClear(ByteBuffer target, ByteBuffer src) {
        copy(target, src);
        src.clear();
    }

    /**
     * 获取{@link ByteBuffer}可读字节数
     */
    public static int getReadableBytes(ByteBuffer target) {
        return target.limit() - target.position();
    }

    /**
     * 获取{@link ByteBuffer}最大可写字节数
     */
    public static int getMaxWritableBytes(ByteBuffer target) {
        return target.capacity() - target.position();
    }
}
