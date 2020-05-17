package org.kin.framework.concurrent.partition.impl;

import org.kin.framework.concurrent.partition.Partitioner;
import org.kin.framework.utils.HashUtils;

/**
 * @author huangjianqin
 * @date 2017/10/26
 * HashTable的Hash方式
 */
public class HashPartitioner<K> implements Partitioner<K> {
    public static final Partitioner INSTANCE = new HashPartitioner();

    @Override
    public int toPartition(K key, int numPartition) {
        return HashUtils.hash(key, numPartition);
    }
}
