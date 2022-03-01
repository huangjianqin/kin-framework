package org.kin.framework.utils;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.sun.nio.zipfs.ZipFileSystem;
import com.sun.nio.zipfs.ZipFileSystemProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 类spi机制, 与spring.factories加载类似, 内容是properties格式
 * !!!!注意, 不会加载java spi service
 *
 * @author huangjianqin
 * @date 2020/9/27
 * @see SPI
 * @see Extension
 */
public class ExtensionLoader {
    private static final Logger log = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String META_INF = "META-INF/";
    /** 默认会加载META-INF, META-INF/kin, META-INF/services下面的kin.factories文件 */
    private static final String DEFAULT_FILE_NAME = "kin.factories";
    /** 默认kin目录路径 */
    private static final String DEFAULT_DIR_NAME = META_INF.concat("kin");
    /** 默认java spi加载目录 */
    private static final String DEFAULT_JAVA_DIR_NAME = META_INF.concat("services");

    /** 自带默认的loader, lazy init */
    private static ExtensionLoader common;

    /** The class loader used to locate, load, and instantiate providers */
    private final ClassLoader classLoader;
    /** The access control context taken when the ServiceLoader is created */
    private final AccessControlContext acc;
    /**
     * key -> extension class name || {@link SPI}注解的值, value -> extension implement class name
     * 一写多读
     */
    private volatile SetMultimap<String, String> extension2ImplClasses = MultimapBuilder.hashKeys().hashSetValues().build();
    /** key -> spi class, value -> extension implement instance */
    private final Map<Class<?>, SpiMetaData<?>> spi2MetaData = new ConcurrentHashMap<>();

    private ExtensionLoader(ClassLoader cl) {
        //取默认的class loader
        classLoader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        //获取security manager
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
    }

    //-------------------------------------------------------------------------------------------------------------------
    public static ExtensionLoader load() {
        return new ExtensionLoader(Thread.currentThread().getContextClassLoader());
    }

    public static ExtensionLoader load(String... fileNames) {
        ExtensionLoader loader = load();
        for (String fileName : fileNames) {
            loader.load(fileName);
        }
        return loader;
    }

    public static ExtensionLoader loadInstalled() {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        return new ExtensionLoader(prev);
    }

    public static ExtensionLoader loadInstalled(String... fileNames) {
        ExtensionLoader loader = loadInstalled();
        for (String fileName : fileNames) {
            loader.load(fileName);
        }
        return loader;
    }

    /** 获取默认的loader */
    public static ExtensionLoader common() {
        if (Objects.isNull(common)) {
            synchronized (ExtensionLoader.class) {
                if (Objects.nonNull(common)) {
                    return common;
                }

                common = load();
                common.loadDefault();
            }
        }

        return common;
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
            //获取其余jar中的目标目录resource
            if (classLoader == null) {
                props = ClassLoader.getSystemResources(fileName);
            } else {
                props = classLoader.getResources(fileName);
            }

            SetMultimap<String, String> extension2ImplClasses = MultimapBuilder.hashKeys().hashSetValues().build();
            extension2ImplClasses.putAll(this.extension2ImplClasses);

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
                        //对于非file schema, 无法用path转换成File, 故通过抽象Path来获取
                        parse(extension2ImplClasses, path);
                        return FileVisitResult.CONTINUE;
                    }
                };
                URI uri = url.toURI();
                String scheme = uri.getScheme();
                FileSystem otherFs = null;
                if (!scheme.equalsIgnoreCase("file")) {
                    //非file schema, 则需要手动加载其file system, 否则解析不出path, 然后就无法遍历目录了
                    //比如jar, 想读取其他jar内的内容, 也不能通过new File读取
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
            this.extension2ImplClasses = extension2ImplClasses;
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 加载默认支持的文件或目录
     */
    public synchronized void loadDefault() {
        load(META_INF.concat(DEFAULT_FILE_NAME));
        load(DEFAULT_DIR_NAME);
        load(DEFAULT_JAVA_DIR_NAME);
    }

    /**
     * 因为需要扫描整个classpath的所有resources, 并且解析后, 这些数据是可以丢弃的,
     * 如果可以释放, 可以减少内存占用. 但是ZipFileSystemProvider并没有把相关方法开放出来(并不想开发者调用??),
     * 故使用反射从其FileSystem缓存中移除
     * <p>
     * TODO: 2022/3/1  兼容JDK17时需要修改该方法逻辑
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
    private void parse(Multimap<String, String> extension2ImplClasses, Path path) {
        try {
            URL url = path.toUri().toURL();
            String fileName = path.getFileName().toString();
            Properties properties = new Properties();
            try (InputStream is = url.openStream()) {
                //加载properties
                properties.load(is);
                for (String extensionClassName : properties.stringPropertyNames()) {
                    String implementClassNamesStr = properties.getProperty(extensionClassName);
                    if (StringUtils.isNotBlank(implementClassNamesStr)) {
                        List<String> implementClassNames = Arrays.asList(implementClassNamesStr.split(","));
                        extension2ImplClasses.putAll(extensionClassName, implementClassNames);

                        if (log.isDebugEnabled()) {
                            for (String implementClassName : implementClassNames) {
                                log.debug("found '{}' implement class '{}'", extensionClassName, implementClassName);
                            }
                        }
                    } else {
                        //只有key, 没有value, 则是没有配置class name或class key, 仅仅配置了implement class name
//                        log.warn("key '{}' doesn't has implement classes", extensionClassName);
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
    private void checkSupport(Class<?> extensionClass) {
        if (!extensionClass.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException(extensionClass.getCanonicalName().concat("doesn't support spi, please ensure @SPI"));
        }
    }

    /**
     * 获取某接口的{@link SpiMetaData}的迭代器
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private <S> SpiMetaData<S> getSpiMetaData(Class<S> spiClass) {
        checkSupport(spiClass);

        //1. 从 接口名 获取该接口实现类
        HashSet<String> filteredSources = new HashSet<>(extension2ImplClasses.get(spiClass.getName()));
        //2. 从 @SPI注解的提供的value 获取该接口实现类
        SPI spi = spiClass.getAnnotation(SPI.class);
        if (Objects.nonNull(spi)) {
            String alias = spi.alias();
            if (StringUtils.isNotBlank(alias)) {
                filteredSources.addAll(extension2ImplClasses.get(alias));
            }
        }

        if (CollectionUtils.isEmpty(filteredSources)) {
            return null;
        }

        synchronized (spiClass) {
            //获取spiMetaData
            return (SpiMetaData<S>) spi2MetaData.computeIfAbsent(spiClass, k -> new SpiMetaData<>(spiClass, new ArrayList<>(filteredSources)));
        }
    }

    /**
     * 根据extension class name | extension simple class name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     */
    public <S> S getExtension(Class<S> spiClass, String name) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByName(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据extension class name | extension simple class name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     */
    public <S> S getExtension(Class<S> spiClass, String name, Object... args) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByName(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 根据extension class name | extension simple class name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     * 如果找不到, 则返回default extension实现类
     */
    public <S> S getExtensionOrDefault(Class<S> spiClass, String name) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByNameOrDefault(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据extension class name | extension simple class name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     * 如果找不到, 则返回default extension实现类
     */
    public <S> S getExtensionOrDefault(Class<S> spiClass, String name, Object... args) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByNameOrDefault(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     */
    public <S> S getExtension(Class<S> spiClass, int code) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByCode((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     */
    public <S> S getExtension(Class<S> spiClass, int code, Object... args) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByCode((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     * 如果找不到, 则返回default extension实现类
     */
    public <S> S getExtensionOrDefault(Class<S> spiClass, int code) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByCodeOrDefault((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     * 如果找不到, 则返回default extension实现类
     */
    public <S> S getExtensionOrDefault(Class<S> spiClass, int code, Object... args) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return null;
        }
        ExtensionMetaData<S> extensionMetaData = spiMetaData.getByCodeOrDefault((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 获取所有extension class实例
     */
    public <S> List<S> getExtensions(Class<S> spiClass) {
        SpiMetaData<S> spiMetaData = getSpiMetaData(spiClass);
        if (Objects.isNull(spiMetaData)) {
            return Collections.emptyList();
        }
        //使用默认构造器创建extension实现类实例
        return spiMetaData.getSortedExtensionMetaDatas().stream().map(ExtensionMetaData::getInstance).collect(Collectors.toList());
    }

    /**
     * {@link SPI}标识的extension接口元数据
     */
    private static class SpiMetaData<E> {
        /** {@link SPI}标识的extension接口 */
        private final Class<E> spiClass;
        /** {@link SPI} */
        private final SPI spi;
        /** 会根据{@link Extension#order()}排序的{@link ExtensionMetaData} list */
        private final List<ExtensionMetaData<E>> sortedExtensionMetaDatas;
        /**
         * key -> extension class name | extension simple class name | {@link Extension#value()}, value -> extension实现类元数据
         * value会存在重复
         */
        private final Map<String, ExtensionMetaData<E>> name2ExtensionMetaData;
        /**
         * key -> {@link Extension#code()}, value -> extension实现类元数据
         * 如果没有开启{@link SPI#coded()}, 则map为null
         */
        private final Map<Byte, ExtensionMetaData<E>> code2ExtensionMetaData;

        /**
         * @param spiClass {@link SPI}标识的extension接口
         * @param sources  实现了{@code spiClass}的extension class全限定类名
         */
        @SuppressWarnings("unchecked")
        public SpiMetaData(Class<E> spiClass, List<String> sources) {
            this.spiClass = spiClass;
            spi = spiClass.getAnnotation(SPI.class);

            List<ExtensionMetaData<E>> extensionMetaDatas = new ArrayList<>();
            Map<String, ExtensionMetaData<E>> name2ExtensionMetaData = new HashMap<>();
            Map<Byte, ExtensionMetaData<E>> code2ExtensionMetaData = null;
            if (isCoded()) {
                code2ExtensionMetaData = new HashMap<>();
            }
            for (String source : sources) {
                Class<? extends E> extensionClass;
                try {
                    extensionClass = (Class<? extends E>) Class.forName(source);
                } catch (ClassNotFoundException e) {
                    ExceptionUtils.throwExt(e);
                    //理论上不会到这里
                    this.sortedExtensionMetaDatas = null;
                    this.name2ExtensionMetaData = null;
                    this.code2ExtensionMetaData = null;
                    return;
                }

                //检查extension class是否实现了spi interface
                if (!spiClass.isAssignableFrom(extensionClass)) {
                    throw new IllegalArgumentException(
                            String.format("fail to load extension '%s', because it is not subtype of '%s'",
                                    extensionClass.getCanonicalName(), spiClass.getCanonicalName()));
                }

                Extension extension = extensionClass.getAnnotation(Extension.class);
                if (isCoded() && extension.code() < 0) {
                    throw new IllegalArgumentException(
                            String.format("fail to load extension '%s', because it's code of @Extension must >=0",
                                    extensionClass.getCanonicalName()));
                }

                ExtensionMetaData<E> extensionMetaData = new ExtensionMetaData<>(extensionClass, isSingleton());
                extensionMetaDatas.add(extensionMetaData);

                //可用names
                Set<String> names = new HashSet<>(4);
                //{@link Extension#value()}
                String extensionAlias = extensionMetaData.getAlias();
                if (StringUtils.isNotBlank(extensionAlias)) {
                    names.add(extensionAlias.toLowerCase());
                }
                //extension simple class name
                names.add(extensionClass.getSimpleName().toLowerCase());
                //extension class name
                names.add(extensionClass.getCanonicalName().toLowerCase());
                names.add(extensionClass.getName().toLowerCase());

                for (String name : names) {
                    ExtensionMetaData<E> oldExtensionMetaData = name2ExtensionMetaData.put(name, extensionMetaData);
                    if (Objects.nonNull(oldExtensionMetaData)) {
                        throw new IllegalArgumentException(
                                String.format("fail to load extension '%s', because it's name is conflict, name=%s, conflict extension class is '%s'",
                                        extensionClass.getCanonicalName(), name, oldExtensionMetaData.getExtensionClass().getCanonicalName()));
                    }
                }

                if (isCoded()) {
                    byte code = extensionMetaData.getCode();
                    //noinspection ConstantConditions
                    ExtensionMetaData<E> oldExtensionMetaData = code2ExtensionMetaData.put(code, extensionMetaData);
                    if (Objects.nonNull(oldExtensionMetaData)) {
                        throw new IllegalArgumentException(
                                String.format("fail to load extension '%s', because it's code of @Extension is conflict, code=%d, conflict extension class is '%s'",
                                        extensionClass.getCanonicalName(), code, oldExtensionMetaData.getExtensionClass().getCanonicalName()));
                    }
                }
            }

            //sort
            Comparator<ExtensionMetaData<E>> comparator = Comparator.comparingInt(ExtensionMetaData::getOrder);
            extensionMetaDatas.sort(comparator.reversed());
            //set
            this.sortedExtensionMetaDatas = Collections.unmodifiableList(extensionMetaDatas);
            this.name2ExtensionMetaData = Collections.unmodifiableMap(name2ExtensionMetaData);
            if (Objects.nonNull(code2ExtensionMetaData)) {
                this.code2ExtensionMetaData = code2ExtensionMetaData;
            } else {
                this.code2ExtensionMetaData = null;
            }
        }

        /**
         * 获取default extension实现类
         */
        @Nullable
        public ExtensionMetaData<E> getDefaultExtension() {
            return getByName(getDefaultExtensionName());
        }

        /**
         * 根据extension class name | extension simple class name | {@link Extension#value()}获取extension实现类元数据
         * 忽略大小写
         */
        @Nullable
        public ExtensionMetaData<E> getByName(String name) {
            if (StringUtils.isBlank(name)) {
                return null;
            }

            Set<String> availableNames = new HashSet<>(3);
            //{@link Extension#value()}
            //默认认为是简称
            availableNames.add(name.toLowerCase());
            //extension simple class name
            availableNames.add(spiClass.getPackage().getName().concat(".").concat(name).toLowerCase());
            //extension class name
            availableNames.add(spiClass.getPackage().getName().concat(".").concat(name).concat(spiClass.getSimpleName()).toLowerCase());

            for (String availableName : availableNames) {
                ExtensionMetaData<E> defaultExtensionMetaData = name2ExtensionMetaData.get(availableName);
                if (Objects.nonNull(defaultExtensionMetaData)) {
                    return defaultExtensionMetaData;
                }
            }

            return null;
        }

        /**
         * 根据extension class name | extension simple class name | {@link Extension#value()}获取extension实现类元数据
         * 忽略大小写
         * 如果找不到, 则返回default extension实现类
         */
        @Nullable
        public ExtensionMetaData<E> getByNameOrDefault(String name) {
            ExtensionMetaData<E> extensionMetaData = null;
            if (StringUtils.isNotBlank(name)) {
                extensionMetaData = getByName(name);
            }
            if (Objects.isNull(extensionMetaData)) {
                extensionMetaData = getDefaultExtension();
            }
            return extensionMetaData;
        }

        /**
         * 根据{@link Extension#code()}获取extension实现类元数据
         */
        @Nullable
        public ExtensionMetaData<E> getByCode(byte code) {
            return code2ExtensionMetaData.get(code);
        }

        /**
         * 根据{@link Extension#code()}获取extension实现类元数据
         * 如果找不到, 则返回default extension实现类
         */
        @Nullable
        public ExtensionMetaData<E> getByCodeOrDefault(byte code) {
            ExtensionMetaData<E> extensionMetaData = code2ExtensionMetaData.get(code);
            if (Objects.isNull(extensionMetaData)) {
                extensionMetaData = getDefaultExtension();
            }
            return extensionMetaData;
        }

        //getter
        public Class<E> getSpiClass() {
            return spiClass;
        }

        public String getDefaultExtensionName() {
            return spi.value();
        }

        public String getAlias() {
            return spi.alias();
        }

        public boolean isCoded() {
            return spi.coded();
        }

        public boolean isSingleton() {
            return spi.singleton();
        }

        public List<ExtensionMetaData<E>> getSortedExtensionMetaDatas() {
            return sortedExtensionMetaDatas;
        }
    }

    /**
     * 配置的且{@link Extension}标识的extension class元数据
     */
    private static class ExtensionMetaData<E> implements Comparable<ExtensionMetaData<E>> {
        /** extension class */
        private final Class<? extends E> claxx;
        /** 是否单例 */
        private final boolean singleton;
        /** {@link Extension}, 可能为null */
        @Nullable
        private final Extension extension;
        /** 单例模式下的extension class instance, lazy init */
        private volatile transient E instance;

        public ExtensionMetaData(Class<? extends E> claxx, boolean singleton) {
            this.claxx = claxx;
            this.singleton = singleton;
            this.extension = claxx.getAnnotation(Extension.class);
        }

        /**
         * 获取extension实现类实例
         */
        @Nonnull
        public E getInstance() {
            return getInstance(null);
        }

        /**
         * 获取extension实现类实例
         */
        @Nonnull
        public E getInstance(Object[] args) {
            if (singleton) {
                //单例
                if (instance == null) {
                    synchronized (this) {
                        if (instance == null) {
                            instance = ClassUtils.instance(claxx, args);
                        }
                    }
                }
                return instance;
            } else {
                return ClassUtils.instance(claxx, args);
            }
        }

        //getter
        public Class<? extends E> getExtensionClass() {
            return claxx;
        }

        public boolean isSingleton() {
            return singleton;
        }

        public byte getCode() {
            return Objects.nonNull(extension) ? extension.code() : -1;
        }

        public int getOrder() {
            return Objects.nonNull(extension) ? extension.order() : 0;
        }

        public String getAlias() {
            return Objects.nonNull(extension) ? extension.value() : "";
        }

        @Override
        public int compareTo(ExtensionMetaData<E> o) {
            return Integer.compare(o.getOrder(), getOrder());
        }
    }
}
