package org.kin.framework.concurrent.partition;

import org.kin.framework.utils.HashUtils;

/**
 * @author huangjianqin
 * @date 2017/10/26
 *
 * HashTable的Hash方式
 */
public class HashPartitioner<K> implements Partitioner<K> {
    public static final Partitioner INSTANCE = new HashPartitioner();

    @Override
    public int toPartition(K key, int partitionNum) {
        return HashUtils.hash(key, partitionNum);
    }
}
