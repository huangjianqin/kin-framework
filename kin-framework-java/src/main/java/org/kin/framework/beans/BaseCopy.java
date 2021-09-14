package org.kin.framework.beans;

import org.kin.framework.utils.ClassUtils;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2021/9/14
 */
abstract class BaseCopy implements Copy {
    /**
     * 实例自身深复制
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Object selfCopy(Object source) {
        Class<?> sourceC = source.getClass();
        if (sourceC.isPrimitive() ||
                String.class.equals(sourceC) || Character.class.equals(sourceC) ||
                Boolean.class.equals(sourceC) || Byte.class.equals(sourceC) ||
                Short.class.equals(sourceC) || Integer.class.equals(sourceC) ||
                Long.class.equals(sourceC) || Float.class.equals(sourceC) ||
                Double.class.equals(sourceC)) {
            //基础类型
            return source;
        } else if (sourceC.isArray()) {
            //数组
            //数组
            Class<?> componentType = sourceC.getComponentType();
            if (componentType.isPrimitive()) {
                //如果数组元素是基础类型, 则直接使用通用接口
                if (Byte.TYPE.equals(componentType)) {
                    byte[] bytes = (byte[]) source;
                    return Arrays.copyOf(bytes, bytes.length);
                } else if (Character.TYPE.equals(componentType)) {
                    char[] chars = (char[]) source;
                    return Arrays.copyOf(chars, chars.length);
                } else if (Float.TYPE.equals(componentType)) {
                    float[] floats = (float[]) source;
                    return Arrays.copyOf(floats, floats.length);
                } else if (Short.TYPE.equals(componentType)) {
                    short[] shorts = (short[]) source;
                    return Arrays.copyOf(shorts, shorts.length);
                } else if (Integer.TYPE.equals(componentType)) {
                    int[] ints = (int[]) source;
                    return Arrays.copyOf(ints, ints.length);
                } else if (Long.TYPE.equals(componentType)) {
                    long[] longs = (long[]) source;
                    return Arrays.copyOf(longs, longs.length);
                } else if (Double.TYPE.equals(componentType)) {
                    double[] doubles = (double[]) source;
                    return Arrays.copyOf(doubles, doubles.length);
                } else if (Boolean.TYPE.equals(componentType)) {
                    boolean[] booleans = (boolean[]) source;
                    return Arrays.copyOf(booleans, booleans.length);
                } else {
                    //没有匹配, 则返回原引用
                    return source;
                }
            } else {
                Object[] arr = ((Object[]) source);
                Object[] newArr = (Object[]) Array.newInstance(componentType, arr.length);
                for (int i = 0; i < arr.length; i++) {
                    newArr[i] = selfCopy(arr[i]);
                }
                return newArr;
            }
        } else if (List.class.isAssignableFrom(sourceC)) {
            //list
            List list = (List) source;
            List newList = new ArrayList(list.size());
            for (Object o : list) {
                newList.add(selfCopy(o));
            }
            return newList;
        } else if (Set.class.isAssignableFrom(sourceC)) {
            //set
            Set set = (Set) source;
            Set newSet = new HashSet(set.size());
            for (Object o : set) {
                newSet.add(selfCopy(o));
            }
            return newSet;
        } else if (Map.class.isAssignableFrom(sourceC)) {
            //map
            Map<Object, Object> map = (Map<Object, Object>) source;
            Map<Object, Object> newMap = new HashMap(map.size());
            for (Map.Entry entry : map.entrySet()) {
                newMap.put(selfCopy(entry.getKey()), selfCopy(entry.getValue()));
            }
            return newMap;
        } else {
            //其他类型
            Object target = ClassUtils.instance(sourceC);
            copyProperties(source, target);
            return target;
        }
    }
}
