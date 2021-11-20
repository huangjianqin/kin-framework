package org.kin.framework.utils;

import java.util.Collection;
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

    public ConcurrentConsistentHash() {
        this(1);
    }

    public ConcurrentConsistentHash(int replicaNum) {
        this(Object::hashCode, Objects::toString, replicaNum);
    }

    public ConcurrentConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum) {
        super(hashFunc, mapper, replicaNum);
    }

    public ConcurrentConsistentHash(Collection<T> objs) {
        this(1, objs);
    }

    public ConcurrentConsistentHash(int replicaNum, Collection<T> objs) {
        this(Object::hashCode, Objects::toString, replicaNum, objs);
    }

    public ConcurrentConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum, Collection<T> objs) {
        super(hashFunc, mapper, replicaNum);
        //初始化节点
        for (T obj : objs) {
            add(circle, obj);
        }
    }

    @Override
    public synchronized void add(T obj) {
        TreeMap<Integer, T> circle = new TreeMap<>(this.circle);
        add(circle, obj);
        this.circle = circle;
    }

    @Override
    public synchronized void remove(T obj) {
        TreeMap<Integer, T> circle = new TreeMap<>(this.circle);
        remove(circle, obj);
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