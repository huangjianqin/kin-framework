package org.kin.framework.io;

import java.nio.ByteBuffer;

/**
 * @author huangjianqin
 * @date 2020/9/28
 */
public final class ByteBufferUtils {
    private ByteBufferUtils() {
    }

    /**
     * 切换为读模式
     * <p>
     * {@param target}本身要处于write mode, 不然存在数据丢失
     * 比如read了一点点数据, 再调用该方法, buffer本身只能再读之前读过的数据
     */
    public static void toReadMode(ByteBuffer target) {
        //change read
        target.flip();
    }

    /**
     * 切换为写模式, 以当前limit开始写数据, 直至达到capacity
     */
    public static void toWriteMode(ByteBuffer target) {
        target.position(target.limit());
        target.limit(target.capacity());
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
    public static int getWritableBytes(ByteBuffer target) {
        return target.capacity() - target.position();
    }

    /**
     * 将{@link ByteBuffer}转换为bytes
     */
    public static byte[] toBytes(ByteBuffer target) {
        int readableBytes = getReadableBytes(target);
        byte[] bytes = new byte[readableBytes];
        target.get(bytes);
        return bytes;
    }

    /**
     * 保证指定{@link ByteBuffer}实例拥有指定可写字节数{@code writableBytes}, 不足则会扩容
     */
    public static ByteBuffer ensureWritableBytes(ByteBuffer source, int writableBytes) {
        int maxWritableBytes = getWritableBytes(source);
        if (maxWritableBytes >= writableBytes) {
            return source;
        }

        int capacity = source.capacity();
        int position = source.position();
        int newCapacity = capacity + writableBytes - maxWritableBytes;
        ByteBuffer ret = source.isDirect() ? ByteBuffer.allocateDirect(newCapacity) :
                ByteBuffer.allocate(newCapacity);

        source.position(0).limit(capacity);
        ret.position(0);
        ret.put(source);
        ret.position(position);
        ret.order(source.order());
        return ret;
    }
}
