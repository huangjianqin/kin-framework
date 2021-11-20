package org.kin.framework.utils;

import java.util.Collection;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * 一致性hash算法
 * 解决扩容, 机器增加减少时, 只会影响附近一个机器的流量, 而不是全部洗牌, 或者减少节点间数据的复制
 *
 * @author huangjianqin
 * @date 2021/11/18
 */
public class ConsistentHash<T> extends AbstractConsistentHash<T> {
    /** hash环 */
    private final TreeMap<Integer, T> circle = new TreeMap<>();

    public ConsistentHash() {
        this(1);
    }

    public ConsistentHash(int replicaNum) {
        this(Object::hashCode, Objects::toString, replicaNum);
    }

    public ConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum) {
        super(hashFunc, mapper, replicaNum);
    }

    public ConsistentHash(Collection<T> objs) {
        this(1, objs);
    }

    public ConsistentHash(int replicaNum, Collection<T> objs) {
        this(Object::hashCode, Objects::toString, replicaNum, objs);
    }

    public ConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum, Collection<T> objs) {
        super(hashFunc, mapper, replicaNum);
        //初始化节点
        for (T obj : objs) {
            add(obj);
        }
    }

    @Override
    public void add(T obj) {
        add(circle, obj);
    }

    @Override
    public void remove(T obj) {
        remove(circle, obj);
    }

    @Override
    public T get(Object o) {
        return get(circle, o);
    }

    @Override
    public String toString() {
        return "ConsistentHash{" +
                "replicaNum=" + replicaNum +
                ", circle=" + circle +
                "} ";
    }
}
