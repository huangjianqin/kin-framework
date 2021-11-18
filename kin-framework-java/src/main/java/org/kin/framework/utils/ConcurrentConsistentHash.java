package org.kin.framework.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * 线程安全的一致性hash算法
 * 适用于读多少写的场景
 * copy on write
 *
 * @author huangjianqin
 * @date 2021/11/19
 */
public class ConcurrentConsistentHash<T> extends AbstractConsistentHash<T> {
    /** hash环 */
    private volatile TreeMap<Integer, T> circle = new TreeMap<>();

    public ConcurrentConsistentHash() {
        this(Collections.emptyList());
    }

    public ConcurrentConsistentHash(Collection<T> nodes) {
        this(1, nodes);
    }

    public ConcurrentConsistentHash(int replicaNum, Collection<T> nodes) {
        this(Object::hashCode, replicaNum, nodes);
    }

    public ConcurrentConsistentHash(Function<Object, Integer> hashFunc, int replicaNum, Collection<T> nodes) {
        super(hashFunc, replicaNum);
        //初始化节点
        for (T node : nodes) {
            add(circle, node);
        }
    }

    @Override
    public synchronized void add(T node) {
        TreeMap<Integer, T> circle = new TreeMap<>(this.circle);
        add(circle, node);
        this.circle = circle;
    }

    @Override
    public synchronized void remove(T node) {
        TreeMap<Integer, T> circle = new TreeMap<>(this.circle);
        remove(circle, node);
        this.circle = circle;
    }

    @Override
    public T get(Object o) {
        return get(circle, o);
    }

    @Override
    public String toString() {
        return "ConcurrentConsistentHash{" +
                "replicaNum=" + replicaNum +
                ", circle=" + circle +
                "} ";
    }
}