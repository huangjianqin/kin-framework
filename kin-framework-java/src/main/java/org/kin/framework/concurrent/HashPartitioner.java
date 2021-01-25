package org.kin.framework.concurrent;

import org.kin.framework.utils.HashUtils;

/**
 * HashTable的Hash方式
 *
 * @author huangjianqin
 * @date 2017/10/26
 */
public class HashPartitioner<K> implements Partitioner<K> {
    public static final Partitioner INSTANCE = new HashPartitioner();

    @Override
    public int toPartition(K key, int partitionNum) {
        return HashUtils.hash(key, partitionNum);
    }
}
