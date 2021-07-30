package org.kin.framework.utils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类spi机制, 与spring.factories加载类似, 内容是properties格式
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class KinServiceLoader {
    private static final Logger log = LoggerFactory.getLogger(KinServiceLoader.class);

    private static final String META_INF = "META-INF/";
    /** 默认会加载META-INF, META-INF/kin, META-INF/services下面的kin.factories文件 */
    private static final String DEFAULT_FILE_NAME = "kin.factories";
    /** 默认kin目录路径 */
    private static final String DEFAULT_DIR_NAME = META_INF.concat("kin");
    /** 默认java spi加载目录 */
    private static final String DEFAULT_JAVA_DIR_NAME = META_INF.concat("services");

    /** The class loader used to locate, load, and instantiate providers */
    private final ClassLoader classLoader;
    /** The access control context taken when the ServiceLoader is created */
    private final AccessControlContext acc;
    /**
     * key -> service class name || {@link SPI}注解的值, value -> service implement class name
     * 一写多读
     */
    private volatile Multimap<String, String> serviceN2ImplementClassN = LinkedListMultimap.create();
    /** key -> service class, value -> service implement instance */
    private Map<Class<?>, ServiceLoader<?>> serviceClass2Loader = new ConcurrentHashMap<>();

    private KinServiceLoader(ClassLoader cl) {
        //取默认的class loader
        classLoader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        //获取security manager
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;

        //加载默认支持的文件或目录
        load(META_INF.concat(DEFAULT_FILE_NAME));
        load(DEFAULT_DIR_NAME);
        load(DEFAULT_JAVA_DIR_NAME);
    }

    //-------------------------------------------------------------------------------------------------------------------
    public static KinServiceLoader load() {
        return new KinServiceLoader(Thread.currentThread().getContextClassLoader());
    }

    public static KinServiceLoader load(String... fileNames) {
        KinServiceLoader loader = load();
        for (String fileName : fileNames) {
            loader.load(fileName);
        }
        return loader;
    }

    public static KinServiceLoader loadInstalled() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        return new KinServiceLoader(prev);
    }

    public static KinServiceLoader loadInstalled(String... fileNames) {
        KinServiceLoader loader = loadInstalled();
        for (String fileName : fileNames) {
            loader.load(fileName);
        }
        return loader;
    }
    //-------------------------------------------------------------------------------------------------------------------

    /**
     * 重新加载
     *
     * @param fileName 可以是file也可以是directory
     */
    public synchronized void load(String fileName) {
        Enumeration<URL> props;
        try {
            if (classLoader == null) {
                props = ClassLoader.getSystemResources(fileName);
            } else {
                props = classLoader.getResources(fileName);
            }

            Multimap<String, String> serviceN2ImplementClassN = LinkedListMultimap.create();
            serviceN2ImplementClassN.putAll(this.serviceN2ImplementClassN);

            //遍历该url下所有items
            while (props.hasMoreElements()) {
                URL url = props.nextElement();

                FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        //对于非file schema, 无法用path转换成File来判断是否是目录, 只能用通用抽象方法
                        if (attrs.isDirectory()) {
                            return FileVisitResult.CONTINUE;
                        }
                        //解析文件
                        //对于非file schema, 无法用path转换成File来获取文件名, 故通过通用抽象Path来获取
                        parse(serviceN2ImplementClassN, path);
                        return FileVisitResult.CONTINUE;
                    }
                };
                URI uri = url.toURI();
                String scheme = uri.getScheme();
                FileSystem otherFs = null;
                if (!scheme.equalsIgnoreCase("file")) {
                    //非file schema, 则需要手动加载其file system, 否则解析不出path, 然后就无法遍历目录了
                    Map<String, String> env = new HashMap<>();
                    env.put("create", "true");
                    otherFs = FileSystems.newFileSystem(uri, env);
                }
                //遍历该url下所有文件
                Path path = Paths.get(uri);
                Files.walkFileTree(path, visitor);
                if (Objects.nonNull(otherFs)) {
                    //扫描完即可移除, 因为其是临时资源, 不释放会浪费内存
                    removeFromZipProvider(uri, otherFs);
                }
            }
            this.serviceN2ImplementClassN = serviceN2ImplementClassN;
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 因为需要扫描整个classpath的所有resources, 并且解析后, 这些数据是可以丢弃的,
     * 如果可以释放, 可以减少内存占用. 但是ZipFileSystemProvider并没有把相关方法开放出来(并不想开发者调用??),
     * 故使用反射从其FileSystem缓存中移除
     */
    @SuppressWarnings("JavaReflectionInvocation")
    private void removeFromZipProvider(URI uri, FileSystem fileSystem) {
        try {
            Class<ZipFileSystemProvider> providerClass = ZipFileSystemProvider.class;
            //获取移除缓存方法
            Method removeMethod = providerClass.getDeclaredMethod("removeFileSystem", Path.class, ZipFileSystem.class);
            if (!removeMethod.isAccessible()) {
                removeMethod.setAccessible(true);
            }
            //该方法将URI转换成的Path, 并作为缓存的key
            Method uriToPathMethod = providerClass.getDeclaredMethod("uriToPath", URI.class);
            if (!uriToPathMethod.isAccessible()) {
                uriToPathMethod.setAccessible(true);
            }
            for (FileSystemProvider installedProvider : FileSystemProvider.installedProviders()) {
                if (installedProvider.getClass().equals(providerClass)) {
                    //remove
                    Path path = (Path) uriToPathMethod.invoke(installedProvider, uri);
                    removeMethod.invoke(installedProvider, path.toRealPath(), fileSystem);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
            log.error("remove temporary file system error!!!", e);
        }
    }

    /**
     * 解析配置文件
     */
    private void parse(Multimap<String, String> serviceN2ImplementClassN, Path path) {
        try {
            URL url = path.toUri().toURL();
            String fileName = path.getFileName().toString();
            Properties properties = new Properties();
            try (InputStream is = url.openStream()) {
                //加载properties
                properties.load(is);
                for (String serviceClassName : properties.stringPropertyNames()) {
                    String implementClassNames = properties.getProperty(serviceClassName);
                    if (StringUtils.isNotBlank(implementClassNames)) {
                        HashSet<String> filtered = new HashSet<>(Arrays.asList(implementClassNames.split(",")));
                        serviceN2ImplementClassN.putAll(serviceClassName, filtered);
                    } else {
                        //只有key, 没有value, 则是没有配置class name或class key, 仅仅配置了implement class name
                        serviceN2ImplementClassN.put(fileName, serviceClassName);
                    }
                }
            }
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 判断是否支持SPI机制
     */
    private void checkSupport(Class<?> serviceClass) {
        if (!serviceClass.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException(serviceClass.getCanonicalName().concat("doesn't support spi, please ensure @SPI"));
        }
    }

    /**
     * 获取某接口的{@link ServiceLoader}的迭代器
     */
    @SuppressWarnings("unchecked")
    private synchronized <S> Iterator<S> iterator(Class<S> serviceClass) {
        checkSupport(serviceClass);
        //1. 从 接口名 获取该接口实现类
        HashSet<String> filtered = new HashSet<>(serviceN2ImplementClassN.get(serviceClass.getCanonicalName()));
        //2. 从 @SPI注解的提供的value 获取该接口实现类
        SPI spi = serviceClass.getAnnotation(SPI.class);
        if (Objects.nonNull(spi)) {
            String key = spi.key();
            if (StringUtils.isNotBlank(key)) {
                filtered.addAll(serviceN2ImplementClassN.get(key));
            }
        }

        //获取service loader
        ServiceLoader<S> newLoader = new ServiceLoader<>(serviceClass, new ArrayList<>(filtered));
        ServiceLoader<?> loader = serviceClass2Loader.putIfAbsent(serviceClass, newLoader);
        if (Objects.isNull(loader)) {
            //本来没有值
            loader = newLoader;
        }
        return (Iterator<S>) loader.iterator();
    }

    /**
     * 获取合适的扩展service类
     */
    public synchronized <S> S getAdaptiveExtension(Class<S> serviceClass) {
        return getAdaptiveExtension(serviceClass, () -> null);
    }

    /**
     * 获取合适的扩展service类
     */
    public synchronized <S> S getAdaptiveExtension(Class<S> serviceClass, Class<S> defaultServiceClass) {
        return getAdaptiveExtension(serviceClass, () -> ClassUtils.instance(defaultServiceClass));
    }

    /**
     * 获取合适的扩展service类
     */
    private <S> S getAdaptiveExtension(Class<S> serviceClass, Callable<S> serviceGetter) {
        checkSupport(serviceClass);
        String defaultServiceName = "";
        SPI spi = serviceClass.getAnnotation(SPI.class);
        if (Objects.nonNull(spi)) {
            defaultServiceName = spi.value();
        }

        Iterator<S> serviceIterator = iterator(serviceClass);
        while (serviceIterator.hasNext()) {
            S implService = serviceIterator.next();
            String implServiceSimpleName = implService.getClass().getSimpleName();
            if (StringUtils.isNotBlank(defaultServiceName) &&
                    //扩展service class name |
                    (defaultServiceName.equalsIgnoreCase(implService.getClass().getCanonicalName()) ||
                            //service simple class name |
                            defaultServiceName.equalsIgnoreCase(implServiceSimpleName) ||
                            //前缀 + service simple class name |
                            defaultServiceName.concat(serviceClass.getSimpleName()).equalsIgnoreCase(implServiceSimpleName))) {
                return implService;
            }
        }

        //找不到任何符合的扩展service类, return 默认
        if (StringUtils.isBlank(defaultServiceName) && Objects.nonNull(serviceGetter)) {
            try {
                return serviceGetter.call();
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }

        throw new IllegalStateException("can not find Adaptive service for" + serviceClass.getCanonicalName());
    }

    /**
     * 获取所有扩展service类
     */
    public synchronized <S> List<S> getExtensions(Class<S> serviceClass) {
        checkSupport(serviceClass);

        List<S> services = new ArrayList<>();
        Iterator<S> serviceIterator = iterator(serviceClass);
        while (serviceIterator.hasNext()) {
            S implService = serviceIterator.next();
            services.add(implService);
        }

        return services;
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
                final Iterator<Map.Entry<String, S>> knownProviders
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
                    throw new ServiceConfigurationError(String.format("%s: provider %s not found", service.getCanonicalName(), cn));
                }

                if (!service.isAssignableFrom(c)) {
                    throw new ServiceConfigurationError(String.format("%s: provider %s not a subtype", service.getCanonicalName(), cn));
                }
                try {
                    S p = service.cast(c.newInstance());
                    providers.put(cn, p);
                    index++;
                    return p;
                } catch (Throwable x) {
                    throw new ServiceConfigurationError(String.format("%s: provider %s could not be instantiated", service.getCanonicalName(), cn), x);
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
