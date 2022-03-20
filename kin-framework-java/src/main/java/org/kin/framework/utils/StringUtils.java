package org.kin.framework.utils;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2018/5/25
 */
public class StringUtils {
    private static final String DELIMITER = ",";
    private static final String KV_DELIMITER = "=";

    /**
     * 字符串是否为空(null or 空串)
     */
    public static boolean isBlank(String s) {
        return s == null || "".equals(s.trim());
    }

    /**
     * 字符串是否非空(null or 空串)
     */
    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    /**
     * 字符串反转
     */
    public static String reverse(String s) {
        if (isNotBlank(s)) {
            return new StringBuilder(s).reverse().toString();
        }
        return s;
    }

    /**
     * 数组格式化
     */
    @SafeVarargs
    public static <E> String mkString(E... contents) {
        return mkString(DELIMITER, contents);
    }

    /**
     * 数组格式化
     */
    @SafeVarargs
    public static <E> String mkString(String delimiter, E... contents) {
        return mkString(delimiter, CollectionUtils.toList(contents), Object::toString);
    }

    /**
     * 数组格式化
     */
    @SafeVarargs
    public static <E> String mkString(Function<E, String> mapper, E... contents) {
        return mkString(DELIMITER, CollectionUtils.toList(contents), mapper);
    }

    /**
     * 数组格式化
     */
    @SafeVarargs
    public static <E> String mkString(String delimiter, Function<E, String> mapper, E... contents) {
        return mkString(delimiter, CollectionUtils.toList(contents), mapper);
    }

    /**
     * 集合格式化
     */
    public static <E> String mkString(Collection<E> collection) {
        return mkString(DELIMITER, collection, Object::toString);
    }

    /**
     * 集合格式化
     */
    public static <E> String mkString(Collection<E> collection, Function<E, String> mapper) {
        return mkString(DELIMITER, collection, mapper);
    }

    /**
     * 集合格式化
     */
    public static <E> String mkString(String delimiter, Collection<E> collection) {
        return mkString(delimiter, collection, Object::toString);
    }

    /**
     * 集合格式化
     */
    public static <E> String mkString(String delimiter, Collection<E> collection, Function<E, String> mapper) {
        if (CollectionUtils.isNonEmpty(collection)) {
            return collection
                    .stream()
                    .map(mapper)
                    .collect(Collectors.joining(delimiter));
        }
        return "";
    }

    /**
     * map格式化
     */
    public static <K, V> String mkString(Map<K, V> map) {
        return mkString(DELIMITER, map);
    }

    /**
     * map格式化
     */
    public static <K, V> String mkString(String delimiter,
                                         Map<K, V> map) {
        return mkString(delimiter, map, KV_DELIMITER, Object::toString, Object::toString);
    }

    /**
     * map格式化
     */
    public static <K, V> String mkString(Map<K, V> map,
                                         String kvDelimiter,
                                         Function<K, String> keyMapper,
                                         Function<V, String> valueMapper) {
        return mkString(DELIMITER, map, kvDelimiter, keyMapper, valueMapper);
    }

    /**
     * map格式化
     */
    public static <K, V> String mkString(String delimiter,
                                         Map<K, V> map,
                                         String kvDelimiter,
                                         Function<K, String> keyMapper,
                                         Function<V, String> valueMapper) {
        if (CollectionUtils.isNonEmpty(map)) {
            return map.entrySet()
                    .stream()
                    .map(entry -> keyMapper.apply(entry.getKey()).concat(kvDelimiter)
                            .concat(valueMapper.apply(entry.getValue())))
                    .collect(Collectors.joining(delimiter));
        }
        return "";
    }

    /**
     * 首字母大写
     */
    public static String firstUpperCase(String s) {
        char[] chars = s.toCharArray();
        if (chars[0] >= 'a' && chars[0] <= 'z') {
            chars[0] = (char) (chars[0] - 32);
        }
        return new String(chars);
    }

    /**
     * 首字母小写
     */
    public static String firstLowerCase(String s) {
        char[] chars = s.toCharArray();
        if (chars[0] >= 'A' && chars[0] <= 'Z') {
            chars[0] = (char) (chars[0] + 32);
        }
        return new String(chars);
    }

    /**
     * 字符串转16进制字符串
     */
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
        }
        return sb.toString().trim();
    }

    /**
     * 16进制字符串转字符串
     */
    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    /**
     * str移除指定后缀suffix
     */
    public static String removeSuffix(String origin, String suffix) {
        if (!isBlank(origin) && !isBlank(suffix)) {
            if (origin.endsWith(suffix)) {
                return origin.substring(0, origin.length() - suffix.length());
            } else {
                return origin;
            }
        } else {
            return origin;
        }
    }

    /**
     * 判断字符串是否为数字
     */
    public static boolean isNumeric(String str) {
        char[] chars = str.toCharArray();
        int idx = 0;
        //判断第一个char是否是-, 因为存在可能是负数
        char first = chars[idx];
        if (first == '-') {
            idx = 1;
        }

        for (int i = idx; i < chars.length; i++) {
            if (!Character.isDigit(chars[i])) {
                return false;
            }
        }

        return true;
    }
}
