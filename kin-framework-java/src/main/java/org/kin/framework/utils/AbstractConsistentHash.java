package org.kin.framework.utils;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2021/11/19
 */
public abstract class AbstractConsistentHash<T> {
    /** 自定义hash算法 */
    protected final Function<Object, Integer> hashFunc;
    /**
     * 虚拟节点数量, 用于节点分布更加均匀, 负载更加均衡
     */
    protected final int replicaNum;

    public AbstractConsistentHash(Function<Object, Integer> hashFunc, int replicaNum) {
        this.hashFunc = hashFunc;
        this.replicaNum = replicaNum;
    }

    /**
     * 增加节点
     * 每增加一个节点，都会在闭环上增加给定数量的虚拟节点
     * <p>
     * 使用hash(toString()+i)来定义节点的slot位置
     */
    public abstract void add(T node);

    protected final void add(TreeMap<Integer, T> circle, T node) {
        for (int i = 0; i < replicaNum; i++) {
            circle.put(hashFunc.apply(node.toString() + i), node);
        }
    }

    /**
     * 移除节点, 同时移除相应的虚拟节点
     */
    public abstract void remove(T node);

    protected final void remove(TreeMap<Integer, T> circle, T node) {
        for (int i = 0; i < replicaNum; i++) {
            circle.remove(hashFunc.apply(node.toString() + i));
        }
    }

    public abstract T get(Object o);

    /**
     * 1. hash({@code o})
     * 2. 取得顺时针方向上最近的节点
     */
    protected final T get(TreeMap<Integer, T> circle, Object o) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hashFunc.apply(o);
        if (!circle.containsKey(hash)) {
            //返回此映射的部分视图，其键大于等于 hash
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        //正好命中
        return circle.get(hash);
    }
}
