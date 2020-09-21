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
        return mkString(MKSTRING_SEPARATOR, contents);
    }

    /**
     * 数组格式化
     */
    @SafeVarargs
    public static <E> String mkString(String separator, E... contents) {
        return mkString(separator, Arrays.asList(contents));
    }

    /**
     * 集合格式化
     */
    public static <E> String mkString(Collection<E> collection) {
        return mkString(MKSTRING_SEPARATOR, collection);
    }

    /**
     * 集合格式化
     */
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

    /**
     * map格式化
     */
    public static <K, V> String mkString(Map<K, V> map) {
        return mkString(MKSTRING_SEPARATOR, map);
    }

    /**
     * map格式化
     */
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
        StringBuffer sb = new StringBuffer("");
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
}
