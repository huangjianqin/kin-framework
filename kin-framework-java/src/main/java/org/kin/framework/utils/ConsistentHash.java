package org.kin.framework.utils;

import java.util.Objects;
import java.util.function.Function;

/**
 * 支持自定义hash算法的一致性hash算法实现
 *
 * @author huangjianqin
 * @date 2022/3/26
 */
public class ConsistentHash<T> extends AbstractConsistentHash<T> {
    /** 基于murmur hash3的hash算法 */
    private static final Function<String, Long> MURMUR_HASH3 = MurmurHash3::hash64;

    /** 自定义hash算法 */
    private final Function<String, Long> hashFunc;

    public ConsistentHash() {
        this(1);
    }

    public ConsistentHash(int replicaNum) {
        this(Objects::toString, replicaNum);
    }

    public ConsistentHash(Function<T, String> mapper) {
        this(mapper, 1);
    }

    public ConsistentHash(Function<T, String> mapper, int replicaNum) {
        this(mapper, replicaNum, MURMUR_HASH3);
    }

    public ConsistentHash(Function<T, String> mapper, int replicaNum, Function<String, Long> hashFunc) {
        super(mapper, replicaNum);
        if (Objects.isNull(hashFunc)) {
            //使用MurmurHash3算法, 会使得hash值随机分布更好, 最终体现是节点hash值均匀散落在哈希环
            hashFunc = MURMUR_HASH3;
        }
        this.hashFunc = hashFunc;
    }

    @Override
    protected long hash(String s) {
        return hashFunc.apply(s);
    }
}
