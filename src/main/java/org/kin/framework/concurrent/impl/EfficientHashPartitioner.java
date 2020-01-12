package org.kin.framework.concurrent.impl;

import org.kin.framework.concurrent.Partitioner;
import org.kin.framework.utils.HashUtils;

/**
 * Created by huangjianqin on 2018/11/5.
 */
public class EfficientHashPartitioner<K> implements Partitioner<K> {
    public static final Partitioner INSTANCE = new EfficientHashPartitioner();

    @Override
    public int toPartition(K key, int numPartition) {
        return HashUtils.efficientHash(key, numPartition);
    }
}
