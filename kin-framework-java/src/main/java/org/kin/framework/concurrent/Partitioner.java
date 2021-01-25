package org.kin.framework.concurrent;

/**
 * 分区算法实现
 *
 * @author huangjianqin
 * @date 2017/10/26
 */
@FunctionalInterface
public interface Partitioner<K> {
    /**
     * 获取分区
     *
     * @param key          key
     * @param partitionNum 分区数
     * @return 分区id
     */
    int toPartition(K key, int partitionNum);
}
