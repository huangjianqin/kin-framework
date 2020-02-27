package org.kin.framework.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2018/5/25
 */
public class StringUtils {
    private static final String MKSTRING_SEPARATOR = ",";

    public static boolean isBlank(String s) {
        return s == null || "".equals(s.trim());
    }

    public static boolean isNotBlank(String s) {
        return !isBlank(s);
    }

    public static String reverse(String s) {
        if (isNotBlank(s)) {
            return new StringBuilder(s).reverse().toString();
        }
        return s;
    }

    public static <E> String mkString(E... contents) {
        return mkString(MKSTRING_SEPARATOR, contents);
    }

    public static <E> String mkString(String separator, E... contents) {
        return mkString(separator, Arrays.asList(contents));
    }

    public static <E> String mkString(Collection<E> collection) {
        return mkString(MKSTRING_SEPARATOR, collection);
    }

    public static <E> String mkString(String separator, Collection<E> collection) {
        if (collection != null && collection.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (E e : collection) {
                sb.append(e).append(separator);
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
        return "";
    }

    public static <K, V> String mkString(Map<K, V> map) {
        return mkString(MKSTRING_SEPARATOR, map);
    }

    public static <K, V> String mkString(String separator, Map<K, V> map) {
        if (map != null && map.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<K, V> entry : map.entrySet()) {
                sb.append("(").append(entry.getKey()).append("-").append(entry.getValue()).append(")").append(separator);
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }
        return "";
    }

    public static String firstUpperCase(String s) {
        char[] chars = s.toCharArray();
        if (chars[0] >= 'a' && chars[0] <= 'z') {
            chars[0] = (char) (chars[0] - 32);
        }
        return new String(chars);
    }

    public static String firstLowerCase(String s) {
        char[] chars = s.toCharArray();
        if (chars[0] >= 'A' && chars[0] <= 'Z') {
            chars[0] = (char) (chars[0] + 32);
        }
        return new String(chars);
    }
}
