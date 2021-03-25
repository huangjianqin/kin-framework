package org.kin.framework.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.checkerframework.checker.units.qual.C;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2019-12-28
 */
public class JSON {
    public static final ObjectMapper PARSER = new ObjectMapper();

    static {
        PARSER.setTypeFactory(TypeFactory.defaultInstance());
        PARSER.findAndRegisterModules();
    }

    private JSON() {
    }

    /**
     * 序列化
     *
     * @param obj 序列化实例
     * @return json字符串
     */
    public static String write(Object obj) {
        try {
            return PARSER.writeValueAsString(obj);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 序列化
     *
     * @param obj 序列化实例
     * @return bytes
     */
    public static byte[] writeBytes(Object obj) {
        try {
            return PARSER.writeValueAsBytes(obj);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析json
     *
     * @param jsonStr json字符串
     * @param clazz   类型
     */
    public static <T> T read(String jsonStr, Class<T> clazz) {
        try {
            return PARSER.readValue(jsonStr, clazz);
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析json
     *
     * @param bytes json bytes
     * @param clazz 反序列化类型
     */
    public static <T> T read(byte[] bytes, Class<T> clazz) {
        try {
            return PARSER.readValue(bytes, clazz);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析含范型参数类型的json
     *
     * @param jsonStr          json字符串
     * @param parametrized     反序列化类
     * @param parameterClasses 范型参数类型
     * @param <T>              类型参数
     */
    public static <T> T read(String jsonStr, Class<T> parametrized, Class<?>... parameterClasses) {
        try {
            JavaType javaType = PARSER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return PARSER.readValue(jsonStr, javaType);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析含范型参数类型的json
     *
     * @param jsonStr       json字符串
     * @param typeReference 类型
     */
    public static <T> T read(String jsonStr, TypeReference<T> typeReference) {
        try {
            return PARSER.readValue(jsonStr, typeReference);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析list json
     *
     * @param jsonStr   json字符串
     * @param itemClass 元素类型
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> readList(String jsonStr, Class<T> itemClass) {
        return readCollection(jsonStr, ArrayList.class, itemClass);
    }

    /**
     * 解析set json
     *
     * @param jsonStr   json字符串
     * @param itemClass 元素类型
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> readSet(String jsonStr, Class<T> itemClass) {
        return readCollection(jsonStr, HashSet.class, itemClass);
    }

    /**
     * 解析集合类json
     *
     * @param jsonStr         json字符串
     * @param collectionClass 集合类型
     * @param itemClass       元素类型
     */
    public static <C extends Collection<T>, T> C readCollection(String jsonStr, Class<C> collectionClass, Class<T> itemClass) {
        JavaType collectionType = PARSER.getTypeFactory().constructCollectionLikeType(collectionClass, itemClass);
        try {
            return PARSER.readValue(jsonStr, collectionType);
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析map json
     *
     * @param jsonStr    json字符串
     * @param keyClass   key类型
     * @param valueClass value类型
     */
    public static <K, V> C readMap(String jsonStr, Class<K> keyClass, Class<V> valueClass) {
        JavaType mapType = PARSER.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        try {
            return PARSER.readValue(jsonStr, mapType);
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 将json形式的map数据转换成对象
     */
    public static <C> C convert(Object jsonObj, Class<? extends C> targetClass) {
        return PARSER.convertValue(jsonObj, targetClass);
    }

    /**
     * 从json bytes 字段数据更新现有Obj实例中的字段值
     */
    public static void updateFieldValue(byte[] bytes, Object object) {
        try {
            PARSER.readerForUpdating(object).readValue(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 从json字符串字段数据更新现有Obj实例中的字段值
     */
    public static void updateFieldValue(String text, Object object) {
        try {
            PARSER.readerForUpdating(object).readValue(text);
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }
    }
}
