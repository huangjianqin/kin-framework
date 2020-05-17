package org.kin.framework.concurrent.partition.impl;

import org.kin.framework.concurrent.partition.Partitioner;
import org.kin.framework.utils.HashUtils;

/**
 * @author huangjianqin
 * @date 2018/11/5
 */
public class EfficientHashPartitioner<K> implements Partitioner<K> {
    public static final Partitioner INSTANCE = new EfficientHashPartitioner();

    @Override
    public int toPartition(K key, int numPartition) {
        return HashUtils.efficientHash(key, numPartition);
    }
}
