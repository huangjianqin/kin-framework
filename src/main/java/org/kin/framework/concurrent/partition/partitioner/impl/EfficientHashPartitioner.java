package org.kin.framework.concurrent.partition.partitioner.impl;

import org.kin.framework.concurrent.partition.partitioner.Partitioner;
import org.kin.framework.utils.HashUtils;

/**
 * @author huangjianqin
 * @date 2018/11/5
 */
public class EfficientHashPartitioner<K> implements Partitioner<K> {
    public static final Partitioner INSTANCE = new EfficientHashPartitioner();

    @Override
    public int toPartition(K key, int partitionNum) {
        return HashUtils.efficientHash(key, partitionNum);
    }
}
