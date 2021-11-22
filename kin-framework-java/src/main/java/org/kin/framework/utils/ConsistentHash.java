package org.kin.framework.utils;

import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2021/11/18
 */
public class ConsistentHash<T> extends AbstractConsistentHash<T> {
    /** hash环 */
    private final TreeMap<Integer, T> circle = new TreeMap<>();

    protected ConsistentHash() {
        this(1);
    }

    protected ConsistentHash(int replicaNum) {
        this(Object::hashCode, Objects::toString, replicaNum);
    }

    protected ConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum) {
        super(hashFunc, mapper, replicaNum);
    }

    @Override
    public void add(T obj, int weight) {
        add(circle, obj, weight);
    }

    @Override
    public void remove(T obj, int weight) {
        remove(circle, obj, weight);
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
