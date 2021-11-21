package org.kin.framework.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    /** 判断两集合是否一致 */
    public static <T> boolean isSame(Collection<T> source, Collection<T> other) {
        return source.size() == other.size() && source.containsAll(other) && other.containsAll(source);
    }
}
