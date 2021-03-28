package org.kin.framework.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2019/7/6
 */
public class CollectionUtils {
    public static <E> boolean isEmpty(Collection<E> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <E> boolean isNonEmpty(Collection<E> collection) {
        return !isEmpty(collection);
    }

    public static <E> boolean isEmpty(E[] array) {
        return array == null || array.length <= 0;
    }

    public static <E> boolean isNonEmpty(E[] array) {
        return !isEmpty(array);
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static <K, V> boolean isNonEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    public static <ITEM> List<ITEM> toList(ITEM[] array) {
        List<ITEM> list = new ArrayList<>();
        for (ITEM item : array) {
            list.add(item);
        }
        return list;
    }

    /**
     * 针对{@link ConcurrentHashMap}, 进行线程安全的putIfAbsent操作, 并返回map中的真实value
     */
    public static <K, V> V putIfAbsent(ConcurrentHashMap<K, V> concurrentHashMap, K key, V newValue) {
        V oldValue = concurrentHashMap.putIfAbsent(key, newValue);
        if (Objects.isNull(oldValue)) {
            return newValue;
        } else {
            return oldValue;
        }
    }

    /** 判断两集合是否一致 */
    public static <T> boolean isSame(Collection<T> source, Collection<T> other) {
        return source.size() == other.size() && source.containsAll(other) && other.containsAll(source);
    }
}
