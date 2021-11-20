package org.kin.framework.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2021/11/20
 */
public final class ConsistentHashBuilder<T> {
    /** 自定义hash算法 */
    private Function<Object, Integer> hashFunc;
    /** 自定义{@code T}对象映射逻辑, 映射function返回的结果会用于hash */
    private Function<T, String> mapper;
    /** 虚拟节点数量, 用于节点分布更加均匀, 负载更加均衡 */
    private int replicaNum;

    private ConsistentHashBuilder() {
    }

    public static <T> ConsistentHashBuilder<T> builder() {
        return new ConsistentHashBuilder<T>();
    }

    public ConsistentHashBuilder<T> hashFunc(Function<Object, Integer> hashFunc) {
        this.hashFunc = hashFunc;
        return this;
    }

    public ConsistentHashBuilder<T> setMapper(Function<T, String> mapper) {
        this.mapper = mapper;
        return this;
    }

    public ConsistentHashBuilder<T> setReplicaNum(int replicaNum) {
        this.replicaNum = replicaNum;
        return this;
    }

    public ConsistentHash<T> common(T... objs) {
        return common(Arrays.asList(objs));
    }

    public ConsistentHash<T> common(Collection<T> objs) {
        return new ConsistentHash<>(hashFunc, mapper, replicaNum, objs);
    }

    public ConcurrentConsistentHash<T> concurrent(T... objs) {
        return concurrent(Arrays.asList(objs));
    }

    public ConcurrentConsistentHash<T> concurrent(Collection<T> objs) {
        return new ConcurrentConsistentHash<>(hashFunc, mapper, replicaNum, objs);
    }
}
