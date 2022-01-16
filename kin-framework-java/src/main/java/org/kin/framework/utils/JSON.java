package org.kin.framework.utils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2019-12-28
 */
public class JSON {
    public static final ObjectMapper PARSER = new ObjectMapper();
    /** json array抽象类型 */
    private static final TypeReference<List<JsonNode>> TYPE_JSON_NODE_LIST = new TypeReference<List<JsonNode>>() {
    };

    static {
        PARSER.setTypeFactory(TypeFactory.defaultInstance());
        PARSER.findAndRegisterModules();
        //允许json中含有指定对象未包含的字段
        PARSER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //允许序列化空对象
        PARSER.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //不序列化默认值, 0,false,[],{}等等, 减少json长度
        PARSER.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT);
        //只认field, 那些get set is开头的方法不生成字段
        PARSER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        PARSER.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        PARSER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        PARSER.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
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
     * @param json json字符串
     * @param type 类型
     */
    public static <T> T read(String json, Type type) {
        try {
            return PARSER.readValue(json, PARSER.constructType(type));
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析json
     *
     * @param bytes json bytes
     * @param type  反序列化类型
     */
    public static <T> T read(byte[] bytes, Type type) {
        try {
            return PARSER.readValue(bytes, PARSER.constructType(type));
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析含范型参数类型的json
     *
     * @param json             json字符串
     * @param parametrized     反序列化类
     * @param parameterClasses 范型参数类型
     * @param <T>              类型参数
     */
    public static <T> T read(String json, Class<T> parametrized, Class<?>... parameterClasses) {
        try {
            JavaType javaType = PARSER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return PARSER.readValue(json, javaType);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析含范型参数类型的json
     *
     * @param json          json字符串
     * @param typeReference 类型
     */
    public static <T> T read(String json, TypeReference<T> typeReference) {
        try {
            return PARSER.readValue(json, typeReference);
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析list json
     *
     * @param json      json字符串
     * @param itemClass 元素类型
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> readList(String json, Class<T> itemClass) {
        return readCollection(json, ArrayList.class, itemClass);
    }

    /**
     * 解析set json
     *
     * @param json      json字符串
     * @param itemClass 元素类型
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> readSet(String json, Class<T> itemClass) {
        return readCollection(json, HashSet.class, itemClass);
    }

    /**
     * 解析集合类json
     *
     * @param json            json字符串
     * @param collectionClass 集合类型
     * @param itemClass       元素类型
     */
    public static <C extends Collection<T>, T> C readCollection(String json, Class<C> collectionClass, Class<T> itemClass) {
        JavaType collectionType = PARSER.getTypeFactory().constructCollectionLikeType(collectionClass, itemClass);
        try {
            return PARSER.readValue(json, collectionType);
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 解析map json
     *
     * @param json       json字符串
     * @param keyClass   key类型
     * @param valueClass value类型
     */
    public static <K, V> Map<K, V> readMap(String json, Class<K> keyClass, Class<V> valueClass) {
        JavaType mapType = PARSER.getTypeFactory().constructMapType(HashMap.class, keyClass, valueClass);
        try {
            return PARSER.readValue(json, mapType);
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
    public static void updateFieldValue(String str, Object object) {
        try {
            PARSER.readerForUpdating(object).readValue(str);
        } catch (JsonProcessingException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 按数组形式读取json, 然后再根据指定class反序列每个item
     */
    public static Object[] readJsonArray(InputStream is, Class<?>[] targetClasses) throws IOException {
        Object[] targets = new Object[targetClasses.length];
        List<JsonNode> jsonNodes = PARSER.readValue(is, TYPE_JSON_NODE_LIST);
        for (int i = 0; i < targetClasses.length; i++) {
            targets[i] = PARSER.treeToValue(jsonNodes.get(i), targetClasses[i]);
        }
        return targets;
    }
}
