package org.kin.framework.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

/**
 * 类spi机制, 与spring.factories加载类似, 内容是properties格式
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class KinServiceLoader {
    /** 默认路径 */
    private static final String FILE_NAME = "META-INF/kin.factories";

    /** The class loader used to locate, load, and instantiate providers */
    private final ClassLoader classLoader;
    /** The access control context taken when the ServiceLoader is created */
    private final AccessControlContext acc;
    /** key -> service class name, value -> service implement class name */
    private final Multimap<String, String> service2Implement = HashMultimap.create();
    /** key -> service class, value -> service implement instance */
    private final Multimap<Class<?>, Object> service2ImplementInst = LinkedListMultimap.create();

    private KinServiceLoader(ClassLoader cl) {
        this(FILE_NAME, cl);
    }

    private KinServiceLoader(String fileName, ClassLoader cl) {
        classLoader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
        reload(fileName);
    }

    /**
     * 重新加载
     */
    public synchronized void reload(String fileName) {
        service2Implement.clear();
        service2ImplementInst.clear();

        Enumeration<URL> configs;
        try {
            if (classLoader == null) {
                configs = ClassLoader.getSystemResources(fileName);
            } else {
                configs = classLoader.getResources(fileName);
            }

            while (configs.hasMoreElements()) {
                URL url = configs.nextElement();
                parse(url);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //TODO
//        lookupIterator = new ServiceLoader.LazyIterator(service, loader);
    }

    /**
     * 解析配置文件
     */
    private void parse(URL url) {
        try {
            Properties properties = new Properties();
            properties.load(url.openStream());
            for (String serviceClassName : properties.stringPropertyNames()) {
                String implementClassNames = properties.getProperty(serviceClassName);
                service2Implement.putAll(serviceClassName, Arrays.asList(implementClassNames.split(",")));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //-------------------------------------------------------------------------------------------------------------------
//    private class LazyIterator
//            implements Iterator<S>
//    {
//
//        Class<S> service;
//        ClassLoader loader;
//        Enumeration<URL> configs = null;
//        Iterator<String> pending = null;
//        String nextName = null;
//
//        private LazyIterator(Class<S> service, ClassLoader loader) {
//            this.service = service;
//            this.loader = loader;
//        }
//
//        private boolean hasNextService() {
//            if (nextName != null) {
//                return true;
//            }
//            if (configs == null) {
//                try {
//                    String fullName = PREFIX + service.getName();
//                    if (loader == null)
//                        configs = ClassLoader.getSystemResources(fullName);
//                    else
//                        configs = loader.getResources(fullName);
//                } catch (IOException x) {
//                    fail(service, "Error locating configuration files", x);
//                }
//            }
//            while ((pending == null) || !pending.hasNext()) {
//                if (!configs.hasMoreElements()) {
//                    return false;
//                }
//                pending = parse(service, configs.nextElement());
//            }
//            nextName = pending.next();
//            return true;
//        }
//
//        private S nextService() {
//            if (!hasNextService())
//                throw new NoSuchElementException();
//            String cn = nextName;
//            nextName = null;
//            Class<?> c = null;
//            try {
//                c = Class.forName(cn, false, loader);
//            } catch (ClassNotFoundException x) {
//                fail(service,
//                        "Provider " + cn + " not found");
//            }
//            if (!service.isAssignableFrom(c)) {
//                fail(service,
//                        "Provider " + cn  + " not a subtype");
//            }
//            try {
//                S p = service.cast(c.newInstance());
//                providers.put(cn, p);
//                return p;
//            } catch (Throwable x) {
//                fail(service,
//                        "Provider " + cn + " could not be instantiated",
//                        x);
//            }
//            throw new Error();          // This cannot happen
//        }
//
//        public boolean hasNext() {
//            if (acc == null) {
//                return hasNextService();
//            } else {
//                PrivilegedAction<Boolean> action = new PrivilegedAction<Boolean>() {
//                    public Boolean run() { return hasNextService(); }
//                };
//                return AccessController.doPrivileged(action, acc);
//            }
//        }
//
//        public S next() {
//            if (acc == null) {
//                return nextService();
//            } else {
//                PrivilegedAction<S> action = new PrivilegedAction<S>() {
//                    public S run() { return nextService(); }
//                };
//                return AccessController.doPrivileged(action, acc);
//            }
//        }
//
//        public void remove() {
//            throw new UnsupportedOperationException();
//        }
//
//    }
}
