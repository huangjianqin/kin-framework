package org.kin.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

/**
 * @author huangjianqin
 * @date 2018/2/26
 */
public class SysUtils {
    private static final Logger log = LoggerFactory.getLogger(SysUtils.class);
    public static final int CPU_NUM = Runtime.getRuntime().availableProcessors();

    private static final Pattern INTEGER_PATTERN = Pattern.compile("-?[0-9]+");

    /**
     * @return 合适的cpu核心数
     */
    public static int getSuitableThreadNum() {
        return CPU_NUM * 2 - 1;
    }

    /**
     * @return ContextClassLoader
     */
    public static ClassLoader getContextClassLoader() {
        if (System.getSecurityManager() == null) {
            return Thread.currentThread().getContextClassLoader();
        } else {
            return AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) () -> Thread.currentThread().getContextClassLoader());
        }
    }

    /**
     * @return SystemClassLoader
     */
    public static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged(
                    (PrivilegedAction<ClassLoader>) ClassLoader::getSystemClassLoader);
        }
    }

    /**
     * @return ClassLoader
     */
    public static ClassLoader getClassLoader(final Class<?> clazz) {
        if (System.getSecurityManager() == null) {
            return clazz.getClassLoader();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) clazz::getClassLoader);
        }
    }

    /**
     * @return 判断是否包含指定key的system property
     */
    public static boolean containsSystemProperty(String key) {
        return getSysProperty(key) != null;
    }

    /**
     * @return system property value or {@code null}
     */
    public static String getSysProperty(String key) {
        return getSysProperty(key, null);
    }

    /**
     * @return system property value
     */
    public static String getSysProperty(String key, String def) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key must not be blank");
        }

        String value = null;
        try {
            if (System.getSecurityManager() == null) {
                value = System.getProperty(key);
            } else {
                value = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
            }
        } catch (Exception e) {
            log.error("Unable to retrieve a system property '" + key + "'; default values will be used.", e);
        }

        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * @return boolean类型的system property value
     */
    public static boolean getBoolSysProperty(String key, boolean def) {
        String value = getSysProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
            return false;
        }

        log.error("Unable to parse the boolean system property '" + key + "':" + value + " - " +
                "using the default value: " + def);

        return def;
    }

    /**
     * @return int类型的system property value
     */
    public static int getIntSysProperty(String key, int def) {
        String value = getSysProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (INTEGER_PATTERN.matcher(value).matches()) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                // Ignore
            }
        }

        log.error(
                "Unable to parse the integer system property '" + key + "':" + value + " - " +
                        "using the default value: " + def);

        return def;
    }

    /**
     * @return long类型的system property value
     */
    public static long getLongSysProperty(String key, long def) {
        String value = getSysProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (INTEGER_PATTERN.matcher(value).matches()) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                // Ignore
            }
        }

        log.error(
                "Unable to parse the long integer system property '" + key + "':" + value + " - " +
                        "using the default value: " + def);

        return def;
    }
}
