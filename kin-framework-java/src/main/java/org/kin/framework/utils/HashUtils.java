package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class HashUtils {
    private HashUtils() {

    }

    /**
     * hash %
     *
     * @param key   目标实例
     * @param limit %限制大小
     */
    public static int hash(Object key, int limit) {
        return key == null ? 0 : ((key.hashCode() & Integer.MAX_VALUE) % limit);
    }

    /**
     * hash %
     * HashMap的Hash方式
     * 更高效的hash方式
     *
     * @param key   目标实例
     * @param limit %限制大小
     */
    public static int efficientHash(Object key, int limit) {
        int h;
        //高低位异或 目的是增加hash的复杂度
        return key == null ? 0 : (((h = key.hashCode()) ^ (h >>> 16)) & (limit - 1));
    }


}
