package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2017/10/26
 */
@FunctionalInterface
public interface Partitioner<K> {
    /**
     * 获取分区
     *
     * @param key          key
     * @param numPartition 分区数
     * @return 分区id
     */
    int toPartition(K key, int numPartition);
}
