package org.kin.framework.utils;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    /** 自定义{@code T}对象映射逻辑, 映射function返回的结果会用于hash */
    private final Function<T, String> mapper;
    /** hash环 */
    private final SortedMap<Long, T> circle = new TreeMap<>();
    /** 虚拟节点数量, 用于节点分布更加均匀, 负载更加均衡 */
    private final int replicaNum;
    /** 读写锁 */
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();

    public AbstractConsistentHash(Function<T, String> mapper, int replicaNum) {
        Preconditions.checkArgument(replicaNum > 0, "replicaNum must be greater than 0");
        if (Objects.isNull(mapper)) {
            mapper = Object::toString;
        }
        this.mapper = mapper;
        this.replicaNum = replicaNum;
    }

    /**
     * 更新slot
     *
     * @param circle hash环
     * @param s      node数据转换成的String
     * @param node   node数据
     */
    protected void applySlot(SortedMap<Long, T> circle, String s, T node) {
        circle.put(hash(s), node);
    }

    public final void add(T node) {
        add(node, 1);
    }

    /**
     * 增加节点
     * 每增加一个节点，都会在闭环上增加给定数量的虚拟节点
     * <p>
     * 使用hash(toString()+i)来定义节点的slot位置
     *
     * @param weight 权重, 用于外部干预默认虚拟节点数量
     */
    public final void add(T node, int weight) {
        Preconditions.checkNotNull(node);
        Preconditions.checkArgument(weight > 0, "weight must be greater than 0");

        int finalNum = replicaNum * weight;
        Preconditions.checkArgument(finalNum > 0, "replicaNum * weight must be greater than 0");
        w.lock();
        try {
            for (int i = 0; i < finalNum; i++) {
                applySlot(circle, mapper.apply(node) + "-" + i, node);
            }
        } finally {
            w.unlock();
        }
    }

    /**
     * 移除slot
     *
     * @param circle hash环
     * @param s      node数据转换成的String
     */
    protected void removeSlot(SortedMap<Long, T> circle, String s) {
        circle.remove(hash(s));
    }

    public final void remove(T node) {
        remove(node, 1);
    }

    /**
     * 移除节点, 同时移除相应的虚拟节点
     *
     * @param weight 权重, 用于外部干预默认虚拟节点数量
     */
    public final void remove(T node, int weight) {
        Preconditions.checkNotNull(node);
        Preconditions.checkArgument(weight > 0, "weight must be greater than 0");

        int finalNum = replicaNum * weight;
        Preconditions.checkArgument(finalNum > 0, "replicaNum * weight must be greater than 0");
        w.lock();
        try {
            for (int i = 0; i < finalNum; i++) {
                removeSlot(circle, mapper.apply(node) + "-" + i);
            }
        } finally {
            w.unlock();
        }
    }

    /**
     * 计算hash值
     */
    protected abstract long hash(String s);

    /**
     * 1. hash({@code k})
     * 2. 取得顺时针方向上最近的节点
     */
    public final T get(Object k) {
        if (circle.isEmpty()) {
            return null;
        }

        long hash = hash(k.toString());
        r.lock();
        try {
            if (!circle.containsKey(hash)) {
                //返回此映射的部分视图，其键大于等于 hash
                SortedMap<Long, T> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }
            //正好命中
            return circle.get(hash);
        } finally {
            r.unlock();
        }
    }

    @Override
    public String toString() {
        return "ConsistentHash{" +
                "replicaNum=" + replicaNum +
                ", circle=" + circle +
                "} ";
    }
}
