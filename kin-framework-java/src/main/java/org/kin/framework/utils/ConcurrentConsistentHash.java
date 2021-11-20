package org.kin.framework.utils;

import java.util.Objects;
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

    ConcurrentConsistentHash() {
        this(1);
    }

    ConcurrentConsistentHash(int replicaNum) {
        this(Object::hashCode, Objects::toString, replicaNum);
    }

    ConcurrentConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum) {
        super(hashFunc, mapper, replicaNum);
    }

    @Override
    public synchronized void add(T obj, int weight) {
        TreeMap<Integer, T> circle = new TreeMap<>(this.circle);
        add(circle, obj, weight);
        this.circle = circle;
    }

    @Override
    public synchronized void remove(T obj, int weight) {
        TreeMap<Integer, T> circle = new TreeMap<>(this.circle);
        remove(circle, obj, weight);
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