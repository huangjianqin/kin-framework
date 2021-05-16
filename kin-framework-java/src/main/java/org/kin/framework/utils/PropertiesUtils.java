package org.kin.framework.utils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * @author huangjianqin
 * @date 2019/7/6
 */
public class PropertiesUtils {
    /**
     * 读取properties文件
     * 如果字符串是file:开头, 则是基于相对路径读取properties文件
     * 否则从class path读取properties文件
     */
    public static Properties loadProperties(String propertyFileName) {
        // disk path
        if (propertyFileName.startsWith("file:")) {
            propertyFileName = propertyFileName.substring("file:".length());
            return loadFileProperties(propertyFileName);
        } else {
            return loadClassPathProperties(propertyFileName);
        }
    }

    /**
     * 从class path读取properties文件
     */
    public static Properties loadClassPathProperties(String propertyFileName) {
        InputStream in = null;
        try {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(propertyFileName);
            if (in == null) {
                return null;
            }

            Properties prop = new Properties();
            prop.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return prop;
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    ExceptionUtils.throwExt(e);
                }
            }
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 基于相对路径读取properties文件
     */
    public static Properties loadFileProperties(String propertyFileName) {
        InputStream in = null;
        try {
            // load file location, disk
            File file = new File(propertyFileName);
            if (!file.exists()) {
                return null;
            }

            URL url = new File(propertyFileName).toURI().toURL();
            in = new FileInputStream(url.getPath());

            Properties prop = new Properties();
            prop.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return prop;
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    ExceptionUtils.throwExt(e);
                }
            }
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 基于相对路径写properties文件
     */
    public static boolean writeProperties(Properties properties, String filePathName) {
        return writeProperties(properties, filePathName, null);
    }

    /**
     * 基于相对路径写properties文件
     */
    public static boolean writeProperties(Properties properties, String filePathName, String comment) {
        FileOutputStream out = null;
        try {

            // mk file
            File file = new File(filePathName);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }

            // write data
            out = new FileOutputStream(file, false);
            properties.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), comment);
            return true;
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    ExceptionUtils.throwExt(e);
                }
            }
        }

        return false;
    }

    /**
     * 加载properties内容
     */
    public static Properties loadPropertiesContent(String content) {
        return loadPropertiesContent(null, content);
    }

    /**
     * 加载properties内容, 添加到目标properties, 或者新建一个properties
     */
    public static Properties loadPropertiesContent(Properties target, String content) {
        if (Objects.isNull(target)) {
            target = new Properties();
        }
        try {
            target.load(new StringReader(content));
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
        return target;
    }

    /**
     * 将properties转换成properties content string
     */
    public static String writePropertiesContent(Properties target) {
        return writePropertiesContent(target, null);
    }

    /**
     * 将properties转换成properties content string
     */
    public static String writePropertiesContent(Properties target, String comment) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            target.store(pw, comment);

            return sw.toString();
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        return "";
    }
}
