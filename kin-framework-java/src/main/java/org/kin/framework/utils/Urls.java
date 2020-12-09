package org.kin.framework.utils;

import com.google.common.base.Preconditions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;

/**
 * URL工具类
 *
 * @author huangjianqin
 * @date 2020/10/11
 */
public class Urls {
    /**
     * 获取url对象
     */
    public static URL url(String url) {
        return url(url, null);
    }

    /**
     * 获取url对象
     */
    public static URL url(String url, URLStreamHandler handler) {
        Preconditions.checkNotNull(url, "URL must not be null");
        if (url.startsWith("classpath:")) {
            url = url.substring("classpath:".length());
            return ClassLoader.getSystemClassLoader().getResource(url);
        } else {
            try {
                return new URL(null, url, handler);
            } catch (MalformedURLException var5) {
                try {
                    return (new File(url)).toURI().toURL();
                } catch (MalformedURLException var4) {
                    ExceptionUtils.throwExt(var4);
                }
            }
        }

        throw new IllegalStateException("encounter unknown error");
    }
}
