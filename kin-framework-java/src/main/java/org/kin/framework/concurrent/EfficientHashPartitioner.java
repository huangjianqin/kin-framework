package org.kin.framework.concurrent;

import org.kin.framework.utils.HashUtils;

/**
 * 高效的hash算法
 *
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
