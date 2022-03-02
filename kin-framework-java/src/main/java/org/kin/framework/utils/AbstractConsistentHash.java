package org.kin.framework.utils;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * 一致性hash算法
 * 解决扩容, 机器增加减少时, 只会影响附近一个机器的流量, 而不是全部洗牌, 或者减少节点间数据的复制
 * 适合有状态服务场景
 *
 * @author huangjianqin
 * @date 2021/11/19
 */
public abstract class AbstractConsistentHash<T> {
    /** 自定义hash算法 */
    protected final Function<Object, Integer> hashFunc;
    /** 自定义{@code T}对象映射逻辑, 映射function返回的结果会用于hash */
    protected final Function<T, String> mapper;
    /** 虚拟节点数量, 用于节点分布更加均匀, 负载更加均衡 */
    protected final int replicaNum;

    protected AbstractConsistentHash(Function<Object, Integer> hashFunc, Function<T, String> mapper, int replicaNum) {
        Preconditions.checkArgument(replicaNum > 0, "replicaNum must be greater than 0");
        if (Objects.isNull(hashFunc)) {
            //使用MurmurHash3算法, 会使得hash值随机分布更好, 最终体现是节点hash值均匀散落在哈希环
            hashFunc = o -> MurmurHash3.hash32(o.toString());
        }
        if (Objects.isNull(mapper)) {
            mapper = Object::toString;
        }
        this.hashFunc = hashFunc;
        this.mapper = mapper;
        this.replicaNum = replicaNum;
    }

    public void add(T obj) {
        add(obj, 1);
    }

    /**
     * 增加节点
     * 每增加一个节点，都会在闭环上增加给定数量的虚拟节点
     * <p>
     * 使用hash(toString()+i)来定义节点的slot位置
     *
     * @param weight 权重, 用于外部干预默认虚拟节点数量
     */
    public abstract void add(T obj, int weight);

    protected final void add(TreeMap<Integer, T> circle, T obj, int weight) {
        Preconditions.checkNotNull(obj);
        Preconditions.checkArgument(weight > 0, "weight must be greater than 0");

        int finalNum = replicaNum * weight;
        Preconditions.checkArgument(finalNum > 0, "replicaNum * weight must be greater than 0");
        for (int i = 0; i < finalNum; i++) {
            circle.put(hashFunc.apply(mapper.apply(obj) + i), obj);
        }
    }

    public void remove(T obj) {
        remove(obj, 1);
    }

    /**
     * 移除节点, 同时移除相应的虚拟节点
     *
     * @param weight 权重, 用于外部干预默认虚拟节点数量
     */
    public abstract void remove(T obj, int weight);

    protected final void remove(TreeMap<Integer, T> circle, T obj, int weight) {
        Preconditions.checkNotNull(obj);
        Preconditions.checkArgument(weight > 0, "weight must be greater than 0");

        int finalNum = replicaNum * weight;
        Preconditions.checkArgument(finalNum > 0, "replicaNum * weight must be greater than 0");
        for (int i = 0; i < finalNum; i++) {
            circle.remove(hashFunc.apply(mapper.apply(obj) + i));
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
