package org.kin.framework.utils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类spi机制, 与spring.factories加载类似, 内容是properties格式
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class KinServiceLoader {
    /** 默认路径 */
    private static final String DEFAULT_FILE_NAME = "META-INF/kin.factories";

    /** The class loader used to locate, load, and instantiate providers */
    private final ClassLoader classLoader;
    /** The access control context taken when the ServiceLoader is created */
    private final AccessControlContext acc;
    /** key -> service class name || {@link SPI}注解的值, value -> service implement class name */
    private volatile Multimap<String, String> service2Implement;
    /** key -> service class, value -> service implement instance */
    private volatile Map<Class<?>, ServiceLoader<?>> service2Loader;

    private KinServiceLoader(String fileName, ClassLoader cl) {
        classLoader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
        reload(fileName);
    }

    //-------------------------------------------------------------------------------------------------------------------
    public static KinServiceLoader load() {
        return load(DEFAULT_FILE_NAME, Thread.currentThread().getContextClassLoader());
    }

    public static KinServiceLoader load(String fileName) {
        return load(fileName, Thread.currentThread().getContextClassLoader());
    }

    public static KinServiceLoader loadInstalled(String fileName) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        return load(fileName, prev);
    }

    public static KinServiceLoader load(String fileName, ClassLoader loader) {
        return new KinServiceLoader(fileName, loader);
    }
    //-------------------------------------------------------------------------------------------------------------------

    /**
     * 重新加载
     */
    public synchronized void reload(String fileName) {
        service2Implement = LinkedListMultimap.create();
        service2Loader = new ConcurrentHashMap<>();

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
    }

    /**
     * 解析配置文件
     */
    private void parse(URL url) {
        try {
            Properties properties = new Properties();
            try (InputStream is = url.openStream()) {
                properties.load(is);
                for (String serviceClassName : properties.stringPropertyNames()) {
                    String implementClassNames = properties.getProperty(serviceClassName);
                    HashSet<String> filtered = new HashSet<>(Arrays.asList(implementClassNames.split(",")));
                    service2Implement.putAll(serviceClassName, filtered);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取某接口的{@link ServiceLoader}的迭代器
     */
    public synchronized <S> Iterator<S> iterator(Class<S> serviceClass) {
        //从接口名 或者 @SPI注解的提供的value 获取该接口实现类
        HashSet<String> filtered = new HashSet<>(service2Implement.get(serviceClass.getName()));

        SPI spi = serviceClass.getAnnotation(SPI.class);
        if (Objects.nonNull(spi)) {
            filtered.addAll(service2Implement.get(spi.value()));
        }

        ServiceLoader<S> newLoader = new ServiceLoader<>(serviceClass, new ArrayList<>(filtered));
        ServiceLoader<?> loader = service2Loader.putIfAbsent(serviceClass, newLoader);
        if (Objects.isNull(loader)) {
            //本来没有值
            loader = newLoader;
        }
        return (Iterator<S>) loader.iterator();
    }

    //-------------------------------------------------------------------------------------------------------------------
    private class ServiceLoader<S> implements Iterable<S> {
        /** 接口类 */
        private final Class<S> service;
        /** lazy 加载 */
        private final LazyIterator lookupIterator;
        /** service cache */
        private final LinkedHashMap<String, S> providers = new LinkedHashMap<>();

        private ServiceLoader(Class<S> service, List<String> source) {
            this.service = service;
            this.lookupIterator = new LazyIterator(source);
        }

        @Override
        public Iterator<S> iterator() {
            return new Iterator<S>() {
                Iterator<Map.Entry<String, S>> knownProviders
                        = providers.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    if (knownProviders.hasNext()) {
                        return true;
                    }
                    return lookupIterator.hasNext();
                }

                @Override
                public S next() {
                    if (knownProviders.hasNext()) {
                        return knownProviders.next().getValue();
                    }
                    return lookupIterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        //----------------------------------------------------------------------------------------------------------------

        /**
         * lazy加载接口实现类的迭代器
         */
        private class LazyIterator implements Iterator<S> {
            /** iterator 当前下标 */
            private int index;
            /** 接口实现类名 */
            private final List<String> source;

            public LazyIterator(List<String> source) {
                this.source = source;
            }

            /**
             * @return 是否可以继续迭代
             */
            private boolean hasNextService() {
                return index < source.size();
            }

            /**
             * 下一接口实现类
             */
            private S nextService() {
                if (!hasNextService()) {
                    throw new NoSuchElementException();
                }
                String cn = source.get(index);
                Class<?> c;
                try {
                    c = Class.forName(cn, false, classLoader);
                } catch (ClassNotFoundException x) {
                    throw new ServiceConfigurationError(String.format("%s: Provider %s not found", service.getName(), cn));
                }

                if (!service.isAssignableFrom(c)) {
                    throw new ServiceConfigurationError(String.format("%s: Provider %s not a subtype", service.getName(), cn));
                }
                try {
                    S p = service.cast(c.newInstance());
                    providers.put(cn, p);
                    index++;
                    return p;
                } catch (Throwable x) {
                    throw new ServiceConfigurationError(String.format("%s: Provider %s could not be instantiated", service.getName(), cn), x);
                }
            }

            @Override
            public boolean hasNext() {
                if (acc == null) {
                    return hasNextService();
                } else {
                    PrivilegedAction<Boolean> action = this::hasNextService;
                    return AccessController.doPrivileged(action, acc);
                }
            }

            @Override
            public S next() {
                if (acc == null) {
                    return nextService();
                } else {
                    PrivilegedAction<S> action = this::nextService;
                    return AccessController.doPrivileged(action, acc);
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        }
    }
}
