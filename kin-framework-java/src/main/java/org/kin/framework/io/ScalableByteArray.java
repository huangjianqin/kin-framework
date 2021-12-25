package org.kin.framework.io;

import org.kin.framework.utils.FixEwma;
import org.kin.framework.utils.Maths;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支持动态伸缩的byte[]
 *
 * @author huangjianqin
 * @date 2021/12/15
 */
public final class ScalableByteArray implements Input, Output {
    /** 每次分配数组的容量 */
    private final int allocSize;
    /** 底层byte[] list */
    private List<byte[]> byteArrays = new ArrayList<>();
    /** 当前读的array offset */
    private int readArrOffset;
    /** 下一次读的offset */
    private int readOffset;
    /** 当前写的array offset */
    private int writeArrOffset;
    /** 下一次写的offset */
    private int writeOffset;
    /**
     * 基于历史分配容量, 预测下次写的容量, 进而防止突发高容量而导致分配过多byte[], 当预测容量小于, 可以适当释放部分byte[]
     * todo β值可修改
     */
    private final FixEwma ewma = new FixEwma(0.5);

    public ScalableByteArray() {
        //默认256 bytes
        this(256);
    }

    public ScalableByteArray(int allocSize) {
        if (!Maths.isPower2(allocSize)) {
            //取最接近的2的n次方作为分配的初始容量
            allocSize = Maths.round2Power2(allocSize);
        }
        this.allocSize = allocSize;
        //init one byte[]
        byteArrays.add(new byte[allocSize]);
    }

    /**
     * 校验是否read index <= write index
     */
    private void validReadIndex(int readArrOffset, int readOffset) {
        if (readArrOffset > writeArrOffset || (readArrOffset == writeArrOffset && readOffset > writeOffset)) {
            throw new IndexOutOfBoundsException(String.format("readArrOffset: %d, readOffset: %d, writeArrOffset: %d, writeOffset: %d",
                    readArrOffset, readOffset, writeArrOffset, writeOffset));
        }
    }

    @Override
    public byte readByte() {
        validReadIndex(readArrOffset, readOffset);
        byte[] bytes = byteArrays.get(readArrOffset);
        byte ret = bytes[readOffset++];
        if (readOffset >= allocSize) {
            readArrOffset++;
            readOffset = 0;
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
        //思路: 中间-最后可写+第一剩余未读
        return (writeArrOffset - readArrOffset) * allocSize - (allocSize - writeOffset) + (allocSize - readOffset);
    }

    @Override
    public int readerIndex() {
        return readArrOffset * allocSize + readOffset;
    }

    @Override
    public Input readerIndex(int readerIndex) {
        if (readerIndex < 0) {
            throw new IndexOutOfBoundsException("readerIndex < 0");
        }
        //重置reader index, 根据单个byte[]容量计算即可
        int readArrOffset = readerIndex / allocSize;
        int readOffset = readerIndex % allocSize;
        //计算完后, 校验一下
        validReadIndex(readArrOffset, readOffset);
        //校验成功, 则set
        this.readArrOffset = readArrOffset;
        this.readOffset = readOffset;
        return this;
    }

    @Override
    public boolean readerIndexSupported() {
        return true;
    }

    @Override
    public void writeByte(int value) {
        byte[] bytes = byteArrays.get(writeArrOffset);
        bytes[writeOffset++] = (byte) value;
        if (writeOffset >= allocSize) {
            //超过单个byte[]容量
            expand();
        }
    }

    private void expand() {
        writeArrOffset++;
        writeOffset = 0;
        if (writeArrOffset >= byteArrays.size()) {
            //列表byteArrays没有多余的byte[]可用, 则动态create
            byteArrays.add(new byte[allocSize]);
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
            byte[] bytes = byteArrays.get(writeArrOffset);
            int writableBytes = allocSize - writeOffset;
            int writeBytes = Math.min(writableBytes, len);
            System.arraycopy(value, startIdx, bytes, writeOffset, writeBytes);
            len -= writeBytes;
            startIdx += writeBytes;
            if (writeBytes >= writableBytes) {
                expand();
            } else {
                writeOffset += writeBytes;
            }
        } while (len > 0);
    }

    @Override
    public int writableBytes() {
        //思路: 前面+最后一个
//        return allocSize - writeOffset + (byteArrays.size() - writeArrOffset - 1) * allocSize;
        return Integer.MAX_VALUE;
    }

    /**
     * 写操作完成, 预备读
     */
    public void toRead() {
        //重置read index
        readArrOffset = 0;
        readOffset = 0;
        //记录本次写入的字节数
        ewma.insert(readableBytes());
    }

    /**
     * 预备写
     */
    public void toWrite() {
        //重置read index和write index
        readArrOffset = 0;
        readOffset = 0;
        writeArrOffset = 0;
        writeOffset = 0;

        //预测下次write size, 并尝试释放当前byteArrays中多余的byte[]
        int predictWriteSize = (int) this.ewma.getEwma();
        //下次write byte[]数量
        int byteArrayNum = predictWriteSize / allocSize + 1;
        if (byteArrayNum >= byteArrays.size()) {
            return;
        }

        //释放当前byteArrays中多余的byte[]
        List<byte[]> newByteArrays = new ArrayList<>(byteArrayNum);
        for (int i = 0; i < byteArrayNum; i++) {
            newByteArrays.add(byteArrays.get(i));
        }
        byteArrays = newByteArrays;
    }

    /**
     * 将readable bytes转换成单个byte[]
     */
    public byte[] toByteArray() {
        if (writeArrOffset == 0 && writeOffset == 0) {
            return new byte[0];
        }
        byte[] ret = new byte[readableBytes()];
        int readArrOffset = this.readArrOffset;
        int readOffset = this.readOffset;

        int retOffset = 0;
        while (readArrOffset <= writeArrOffset) {
            int limit;
            if (readArrOffset == writeArrOffset) {
                //最后一个
                if (readOffset == writeOffset) {
                    //相等, 则结束
                    break;
                }
                limit = writeOffset;
            } else {
                //前面的byte[]复制以allocSize为limit即可
                limit = allocSize;
            }
            byte[] bytes = byteArrays.get(readArrOffset);
            //copy bytes len
            int len = limit - readOffset;
            //copy byte[]
            System.arraycopy(bytes, readOffset, ret, retOffset, len);
            //offset ad
            retOffset += len;
            //下一个byte[]
            readArrOffset++;
            readOffset = 0;
        }

        return ret;
    }

    /**
     * 将readable bytes转换成预备可读的{@link ByteBuffer}
     */
    public ByteBuffer toByteBuffer(boolean direct) {
        byte[] bytes = toByteArray();
        int len = bytes.length;
        ByteBuffer byteBuffer;
        if (direct) {
            byteBuffer = ByteBuffer.allocateDirect(len);
        } else {
            byteBuffer = ByteBuffer.allocate(len);
        }
        byteBuffer.put(bytes);
        ByteBufferUtils.toReadMode(byteBuffer);
        return byteBuffer;
    }

    @Override
    public String toString() {
        return "DynamicExpandByteArray{" +
                "allocSize=" + allocSize +
                ", byteArrays=" + byteArrays.stream().map(Arrays::toString).collect(Collectors.toList()) +
                ", readArrOffset=" + readArrOffset +
                ", readOffset=" + readOffset +
                ", writeArrOffset=" + writeArrOffset +
                ", writeOffset=" + writeOffset +
                '}';
    }
}
