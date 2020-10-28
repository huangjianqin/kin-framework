package org.kin.framework.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author huangjianqin
 * @date 2019-12-28
 */
public class JSON {
    private static Logger logger = LoggerFactory.getLogger(JSON.class);

    public static final ObjectMapper PARSER = new ObjectMapper();

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
            logger.error(e.getMessage(), e);
        }
        return null;
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
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 反序列化
     *
     * @param jsonStr json字符串
     * @param clazz   反序列化类型
     * @return 反序列化类实例
     */
    public static <T> T read(String jsonStr, Class<T> clazz) {
        try {
            return PARSER.readValue(jsonStr, clazz);
        } catch (JsonParseException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 反序列化
     *
     * @param jsonStr json字符串
     * @param clazz   反序列化类型
     * @return 反序列化类实例
     */
    public static <T> T read(byte[] bytes, Class<T> clazz) {
        try {
            return PARSER.readValue(bytes, clazz);
        } catch (JsonParseException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 反序列化含范型参数
     *
     * @param jsonStr          json字符串
     * @param parametrized     反序列化类
     * @param parameterClasses 范型参数类型
     * @param <T>              反序列化类型参数
     * @return 反序列化类实例
     */
    public static <T> T read(String jsonStr, Class<T> parametrized, Class<?>... parameterClasses) {
        try {
            JavaType javaType = PARSER.getTypeFactory().constructParametricType(parametrized, parameterClasses);
            return PARSER.readValue(jsonStr, javaType);
        } catch (JsonParseException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 反序列化含范型参数
     *
     * @param jsonStr       json字符串
     * @param typeReference 反序列化类型
     * @return 反序列化类实例
     */
    public static <T> T read(String jsonStr, TypeReference<T> typeReference) {
        try {
            return PARSER.readValue(jsonStr, typeReference);
        } catch (JsonParseException e) {
            logger.error(e.getMessage(), e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}
