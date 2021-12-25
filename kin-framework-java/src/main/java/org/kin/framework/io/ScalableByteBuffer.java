package org.kin.framework.io;

import org.kin.framework.utils.FixEwma;
import org.kin.framework.utils.Maths;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 支持动态伸缩的{@link ByteBuffer}
 *
 * @author huangjianqin
 * @date 2021/12/17
 */
public final class ScalableByteBuffer implements Input, Output {
    /** 每次分配数组的容量 */
    private final int allocSize;
    /** 是否开启mmap */
    private final boolean direct;
    /** 底层{@link ByteBuffer} list */
    private List<ByteBuffer> byteBufferList = new ArrayList<>();
    /** byte buffer offset */
    private int offset;
    /** byte buffer limit */
    private int limit;
    /**
     * 基于历史分配容量, 预测下次写的容量, 进而防止突发高容量而导致分配过多{@link ByteBuffer}, 当预测容量小于, 可以适当释放部分{@link ByteBuffer}
     * todo β值可修改
     */
    private final FixEwma ewma = new FixEwma(0.5);

    public ScalableByteBuffer() {
        this(false);
    }

    public ScalableByteBuffer(boolean direct) {
        //默认256 bytes
        this(256, direct);
    }

    public ScalableByteBuffer(int allocSize) {
        this(allocSize, false);
    }

    public ScalableByteBuffer(int allocSize, boolean direct) {
        if (!Maths.isPower2(allocSize)) {
            //取最接近的2的n次方作为分配的初始容量
            allocSize = Maths.round2Power2(allocSize);
        }
        this.allocSize = allocSize;
        this.direct = direct;
        //init one ByteBuffer
        byteBufferList.add(newByteBuffer());
    }

    private ByteBuffer newByteBuffer(int size) {
        if (direct) {
            return ByteBuffer.allocateDirect(size);
        } else {
            return ByteBuffer.allocate(size);
        }
    }

    private ByteBuffer newByteBuffer() {
        return newByteBuffer(allocSize);
    }

    @Override
    public byte readByte() {
        if (offset > limit || ByteBufferUtils.getReadableBytes(byteBufferList.get(offset)) <= 0) {
            throw new IndexOutOfBoundsException(String.format("offset: %d, offset bytebuffer position: %d, limit: %d, limit bytebuffer limit: %d",
                    offset, byteBufferList.get(offset).position(),
                    limit, byteBufferList.get(limit).limit()));
        }
        ByteBuffer byteBuffer = byteBufferList.get(offset);
        byte ret = byteBuffer.get();
        if (ByteBufferUtils.getReadableBytes(byteBuffer) <= 0) {
            offset++;
        }
        return ret;
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
        while (readableBytes() > 0) {
            dst[dstIndex] = readByte();
        }
        return this;
    }

    @Override
    public int readableBytes() {
        int ret = 0;
        for (int i = offset; i <= limit; i++) {
            ret += ByteBufferUtils.getReadableBytes(byteBufferList.get(i));
        }
        return ret;
    }

    @Override
    public int readerIndex() {
        ByteBuffer byteBuffer = byteBufferList.get(offset);
        if (Objects.isNull(byteBuffer)) {
            return offset * allocSize;
        } else {
            return offset * allocSize + byteBuffer.position();
        }
    }

    @Override
    public Input readerIndex(int readerIndex) {
        if (readerIndex < 0) {
            throw new IndexOutOfBoundsException("readerIndex < 0");
        }
        //重置reader index
        int offset = readerIndex / allocSize;
        int position = readerIndex % allocSize;
        //计算完后, 校验一下
        if (offset > limit || position > byteBufferList.get(offset).limit()) {
            throw new IndexOutOfBoundsException(String.format("new offset: %d, new offset bytebuffer position: %d, limit: %d, limit bytebuffer limit: %d",
                    offset, position,
                    limit, byteBufferList.get(limit).limit()));
        }
        //校验成功, 则set
        this.offset = offset;
        byteBufferList.get(offset).position(position);
        return this;
    }

    @Override
    public boolean readerIndexSupported() {
        return true;
    }

    @Override
    public void writeByte(int value) {
        ByteBuffer byteBuffer = byteBufferList.get(limit);
        byteBuffer.put((byte) value);
        if (ByteBufferUtils.getWritableBytes(byteBuffer) <= 0) {
            //超过单个ByteBuffer容量
            expand();
        }
    }

    /**
     * 扩容
     */
    private void expand() {
        limit++;
        if (limit >= byteBufferList.size()) {
            //没有多余的ByteBuffer可用, 则动态create
            byteBufferList.add(newByteBuffer());
        }
    }

    @Override
    public void writeBytes(byte[] value, int startIdx, int len) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException("value is null");
        }
        if (startIdx < 0) {
            throw new IllegalArgumentException("startIdx is null");
        }
        if (len <= 0) {
            throw new IllegalArgumentException("len is less than or equal to 0");
        }
        do {
            ByteBuffer byteBuffer = byteBufferList.get(limit);
            int writableBytes = ByteBufferUtils.getWritableBytes(byteBuffer);
            int writeBytes = Math.min(writableBytes, len);
            byteBuffer.put(value, startIdx, writeBytes);
            len -= writeBytes;
            startIdx += writeBytes;
            if (writeBytes >= writableBytes) {
                expand();
            }
        } while (len > 0);
    }

    @Override
    public int writableBytes() {
//        int ret = 0;
//        for (int i = limit; i < byteBufferList.size(); i++) {
//            ret += ByteBufferUtils.getWritableBytes(byteBufferList.get(i));
//        }
//        return ret;
        return Integer.MAX_VALUE;
    }

    /**
     * 写操作完成, 预备读
     */
    public void toRead() {
        //重置read index
        offset = 0;
        for (int i = offset; i <= limit; i++) {
            ByteBufferUtils.toReadMode(byteBufferList.get(i));
        }
        //记录本次写入的字节数
        ewma.insert(readableBytes());
    }

    /**
     * 预备写
     */
    public void toWrite() {
        //重置read index和write index
        offset = 0;
        limit = 0;
        for (ByteBuffer byteBuffer : byteBufferList) {
            byteBuffer.clear();
        }

        //预测下次write size, 并尝试释放当前byteBufferList中多余的ByteBuffer
        int predictWriteSize = (int) this.ewma.getEwma();
        //下次write ByteBuffer数量
        int byteBufferNum = predictWriteSize / allocSize + 1;
        if (byteBufferNum >= byteBufferList.size()) {
            return;
        }

        //释放当前byteBufferList中多余的ByteBuffer
        List<ByteBuffer> newByteBufferList = new ArrayList<>(byteBufferNum);
        for (int i = 0; i < byteBufferNum; i++) {
            newByteBufferList.add(byteBufferList.get(i));
        }
        byteBufferList = newByteBufferList;
    }

    /**
     * 将readable bytes转换成单个{@link ByteBuffer}
     */
    public byte[] toByteArray() {
        int readableBytes = readableBytes();
        if (readableBytes <= 0) {
            return new byte[0];
        }
        byte[] ret = new byte[readableBytes];
        int offset = this.offset;
        int arrOffset = 0;
        while (offset <= limit) {
            ByteBuffer byteBuffer = byteBufferList.get(offset);
            //记录当前position
            byteBuffer.mark();
            int singleReadableBytes = ByteBufferUtils.getReadableBytes(byteBuffer);
            //写入所有可读bytes
            byteBuffer.get(ret, arrOffset, singleReadableBytes);
            //恢复之前position
            byteBuffer.reset();
            //数据offset增加可读字节数
            arrOffset += singleReadableBytes;
            offset++;
        }

        return ret;
    }

    /**
     * 将readable bytes转换成预备可读的{@link ByteBuffer}
     */
    public ByteBuffer toByteBuffer(boolean direct) {
        int readableBytes = readableBytes();
        if (readableBytes <= 0) {
            return newByteBuffer(0);
        }
        ByteBuffer ret = newByteBuffer(readableBytes);
        int offset = this.offset;
        while (offset <= limit) {
            ByteBuffer byteBuffer = byteBufferList.get(offset);
            //记录当前position
            byteBuffer.mark();
            //写入所有可读bytes
            ret.put(byteBuffer);
            //恢复之前position
            byteBuffer.reset();
            offset++;
        }

        ByteBufferUtils.toReadMode(ret);
        return ret;
    }

    @Override
    public String toString() {
        return "ScalableByteBuffer{" +
                "allocSize=" + allocSize +
                ", direct=" + direct +
                ", byteBufferList=" + byteBufferList +
                ", offset=" + offset +
                ", limit=" + limit +
                '}';
    }
}
