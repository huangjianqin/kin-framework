package org.kin.framework.utils;

import java.util.Collection;
import java.util.Collections;
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
        this(Collections.emptyList());
    }

    public ConsistentHash(Collection<T> nodes) {
        this(1, nodes);
    }

    public ConsistentHash(int replicaNum, Collection<T> nodes) {
        this(Object::hashCode, replicaNum, nodes);
    }

    public ConsistentHash(Function<Object, Integer> hashFunc, int replicaNum, Collection<T> nodes) {
        super(hashFunc, replicaNum);
        //初始化节点
        for (T node : nodes) {
            add(node);
        }
    }

    @Override
    public void add(T node) {
        add(circle, node);
    }

    @Override
    public void remove(T node) {
        remove(circle, node);
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
