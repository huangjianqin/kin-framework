package org.kin.framework.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.SortedMap;
import java.util.function.Function;

/**
 * 基于ketama的一致性hash算法实现
 *
 * @author huangjianqin
 * @date 2022/3/26
 */
public final class KetamaConsistentHash<T> extends AbstractConsistentHash<T> {
    public KetamaConsistentHash() {
        this(1);
    }

    public KetamaConsistentHash(int replicaNum) {
        this(Objects::toString, replicaNum);
    }

    public KetamaConsistentHash(Function<T, String> mapper) {
        this(mapper, 1);
    }

    public KetamaConsistentHash(Function<T, String> mapper, int replicaNum) {
        super(mapper, replicaNum);
    }

    @Override
    protected void applySlot(SortedMap<Long, T> circle, String s, T node) {
        byte[] digest = md5(s);
        for (int i = 0; i < 4; i++) {
            circle.put(getKetamaKey(digest, i), node);
        }
    }

    @Override
    protected void removeSlot(SortedMap<Long, T> circle, String s) {
        byte[] digest = md5(s);
        for (int i = 0; i < 4; i++) {
            circle.remove(getKetamaKey(digest, i));
        }
    }

    @Override
    protected long hash(String s) {
        return getKetamaKey(s);
    }

    /**
     * 取MD5前4位作为hash
     */
    private static long getKetamaKey(String k) {
        byte[] digest = md5(k);
        return getKetamaKey(digest, 0) & 0xffffffffL;
    }

    /**
     * 因为MD5共16位, 现每次4位, 取4组
     */
    private static Long getKetamaKey(byte[] digest, int i) {
        return ((long) (digest[3 + i * 4] & 0xFF) << 24) | ((long) (digest[2 + i * 4] & 0xFF) << 16) | ((long) (digest[1 + i * 4] & 0xFF) << 8) | (digest[i * 4] & 0xFF);
    }

    /**
     * 基于MD5加密
     * MD5共16位
     */
    private static byte[] md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignore) {
            return s.getBytes(StandardCharsets.UTF_8);
        }
    }
}
