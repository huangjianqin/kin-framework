package org.kin.framework.beans;

import org.kin.framework.collection.CollectionFactories;
import org.kin.framework.collection.MapFactories;
import org.kin.framework.utils.ClassUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 对动态类型的bean copy抽象
 *
 * @author huangjianqin
 * @date 2021/9/14
 */
abstract class PolymorphicCopy implements Copy<Object, Object> {
    /**
     * 指定实例自身深复制
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Object selfCopy(Object source) {
        Class<?> sourceType = source.getClass();
        if (sourceType.isPrimitive() ||
                String.class.equals(sourceType) || Character.class.equals(sourceType) ||
                Boolean.class.equals(sourceType) || Byte.class.equals(sourceType) ||
                Short.class.equals(sourceType) || Integer.class.equals(sourceType) ||
                Long.class.equals(sourceType) || Float.class.equals(sourceType) ||
                Double.class.equals(sourceType)) {
            //基础类型
            return source;
        } else if (sourceType.isArray()) {
            //数组
            Class<?> componentType = sourceType.getComponentType();
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
        } else if (Collection.class.isAssignableFrom(sourceType)) {
            //collection
            Collection collection = (Collection) source;
            Collection newCollection = CollectionFactories.instance().getFactory(sourceType).newCollection();
            for (Object o : collection) {
                newCollection.add(selfCopy(o));
            }
            return newCollection;
        } else if (Map.class.isAssignableFrom(sourceType)) {
            //map
            Map<Object, Object> map = (Map<Object, Object>) source;
            Map<Object, Object> newMap = (Map<Object, Object>) MapFactories.instance().getFactory(sourceType).newMap();
            for (Map.Entry entry : map.entrySet()) {
                newMap.put(selfCopy(entry.getKey()), selfCopy(entry.getValue()));
            }
            return newMap;
        } else if (sourceType.isEnum()) {
            //enum
            return source;
        } else {
            //pojo
            Object target = ClassUtils.instance(sourceType);
            copyProperties(source, target);
            return target;
        }
    }

    /**
     * 获取{@link Copy}实例缓存唯一key
     * 有些{@link Copy}实现需要缓存生成{@link Copy}实例, 用于下次快速实现bean copy
     *
     * @see ByteBuddyBeanCopy
     * @see UnsafeBeanCopy
     */
    protected int cacheKey(Class<?> sourceClass, Class<?> targetClass) {
        //使用hashcode, 节省内存
        return sourceClass.getName().concat("$").concat(targetClass.getName()).hashCode();
    }
}
