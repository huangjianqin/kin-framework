package org.kin.framework.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * spi机制
 * <p>
 * 同时支持类似spring.factories和java service的加载方式
 * 1. extension class及其实现类写入META-INF/kin.factories, 文件内容参考spring.factories
 * 2. 在META-INF/kin和META-INF/services下定义以extension class命名的文件, 并实现类写入这些文件, 文件内容可以是每行一个实现类, 也可以是kv(即spring.factories), 也可以是混合
 * <p>
 * !!!建议使用者直接使用{@link ExtensionLoader}的静态方法, 不要直接操作{@link ExtensionLoader}实例方法
 *
 * @author huangjianqin
 * @date 2020/9/27
 * @see SPI
 * @see Extension
 */
public final class ExtensionLoader<E> {
    private static final Logger log = LoggerFactory.getLogger(ExtensionLoader.class);

    private static final String META_INF = "META-INF/";
    /** kin.factories文件路径 */
    private static final String DEFAULT_FACTORIES_FILE_NAME = META_INF.concat("kin.factories");
    /** 自定义service class定义文件目录 */
    private static final String DEFAULT_DIR_NAME = META_INF.concat("kin");
    /** java service class定义文件目录 */
    private static final String JAVA_SERVICE_DIR_NAME = META_INF.concat("services");

    /**
     * 缓存所有使用的{@link ExtensionLoader}
     * 基于copy-on-write更新
     */
    private static volatile Map<ClassLoader, Map<Class<?>, ExtensionLoader<?>>> classloader2Class2ExtensionLoader = new HashMap<>();
    /** kin.factories内定义的extension class及实现类, 仅仅加载一次 */
    private static volatile SetMultimap<String, String> factoriesExtension2ImplClasses;

    /** The class loader used to locate, load, and instantiate providers */
    private final ClassLoader classLoader;
    /** 带{@link SPI}注解的extension class */
    private final Class<E> extensionClass;
    /** extension class上注解的{@link SPI} */
    private final SPI spi;
    /** extension implement class name */
    private final Set<String> implClassNames;
    /** 根据{@link Extension#order()}排序的{@link ExtensionMetaData}的{@link List} */
    private final List<ExtensionMetaData<E>> sortedExtensionMetaDataList;
    /**
     * key -> extension class name | simple extension class name | {@link Extension#value()}
     * value -> extension实现类元数据, 同一实现类共用同一{@link ExtensionMetaData}实例, 所以value可能会重复
     */
    private final Map<String, ExtensionMetaData<E>> name2ExtensionMetaData;
    /**
     * key -> {@link Extension#code()}, value -> extension实现类元数据
     * 如果没有开启{@link SPI#coded()}, 则map为{@link Collections#emptyMap()}
     */
    private final Map<Byte, ExtensionMetaData<E>> code2ExtensionMetaData;

    private ExtensionLoader(Class<E> extensionClass) {
        this(ExtensionLoader.class.getClassLoader(), extensionClass);
    }

    private ExtensionLoader(ClassLoader classLoader, Class<E> extensionClass) {
        Preconditions.checkNotNull(classLoader);
        Preconditions.checkNotNull(extensionClass);
        checkSupport(extensionClass);

        this.classLoader = classLoader;
        this.extensionClass = extensionClass;
        spi = this.extensionClass.getAnnotation(SPI.class);

        //获取定义的所有extensionClass实现类
        Set<String> implClassNames = new HashSet<>();
        //从META-INF/kin/{extension class name}寻找extensionClass实现类
        implClassNames.addAll(loadFromServiceFile(DEFAULT_DIR_NAME));
        //从META-INF/services/{extension class name}寻找extensionClass实现类
        implClassNames.addAll(loadFromServiceFile(JAVA_SERVICE_DIR_NAME));
        //从META-INF/kin.factories寻找extensionClass实现类
        implClassNames.addAll(getImplClassNamesFromFactories());
        this.implClassNames = Collections.unmodifiableSet(implClassNames);

        //初始化extensionClass实现类元数据
        List<ExtensionMetaData<E>> extensionMetaDataList = new ArrayList<>();
        Map<String, ExtensionMetaData<E>> name2ExtensionMetaData = new HashMap<>();
        Map<Byte, ExtensionMetaData<E>> code2ExtensionMetaData = null;
        if (isCoded()) {
            code2ExtensionMetaData = new HashMap<>();
        }
        initAllExtensionMetaData(implClassNames, extensionMetaDataList, name2ExtensionMetaData, code2ExtensionMetaData);
        this.sortedExtensionMetaDataList = Collections.unmodifiableList(extensionMetaDataList);
        this.name2ExtensionMetaData = Collections.unmodifiableMap(name2ExtensionMetaData);
        if (Objects.nonNull(code2ExtensionMetaData)) {
            this.code2ExtensionMetaData = Collections.unmodifiableMap(code2ExtensionMetaData);
        } else {
            this.code2ExtensionMetaData = Collections.emptyMap();
        }
    }

    //-------------------------------------------------------------------------------------------------------------------

    /**
     * 从META-INF/kin.factories获取指定extension class的实现类
     *
     * @param name extension class name | {@link SPI#alias()}
     */
    private static synchronized Set<String> getImplClassNamesFromFactories(String name) {
        if (Objects.isNull(factoriesExtension2ImplClasses)) {
            //初始化
            ExtensionLoader.factoriesExtension2ImplClasses = ImmutableSetMultimap.copyOf(loadFromFactoriesFile());
        }

        return factoriesExtension2ImplClasses.get(name);
    }

    /**
     * 从META-INF/kin.factories加载extension class及其实现类定义
     */
    private static SetMultimap<String, String> loadFromFactoriesFile() {
        SetMultimap<String, String> factoriesExtension2ImplClasses = MultimapBuilder.hashKeys().hashSetValues().build();
        try {
            Enumeration<URL> props = ClassLoader.getSystemResources(DEFAULT_FACTORIES_FILE_NAME);
            //遍历classpath中所有kin.factories
            while (props.hasMoreElements()) {
                URL url = props.nextElement();
                try (InputStream inputStream = url.openStream()) {
                    parseFactoriesFile(factoriesExtension2ImplClasses, inputStream);
                }
            }
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        return factoriesExtension2ImplClasses;
    }

    /**
     * 解析kin.factories
     * 以{@link Properties}形式读取
     */
    private static void parseFactoriesFile(Multimap<String, String> factoriesExtension2ImplClasses, InputStream inputStream) {
        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            //spring.factories格式, 即key-value格式, 也就是properties
            for (String extensionClassName : properties.stringPropertyNames()) {
                String implementClassNamesStr = properties.getProperty(extensionClassName);
                //支持逗号分割定义多个实现类
                List<String> implementClassNames = Arrays.asList(implementClassNamesStr.split(","));
                factoriesExtension2ImplClasses.putAll(extensionClassName, implementClassNames);

                if (log.isDebugEnabled()) {
                    for (String implementClassName : implementClassNames) {
                        log.debug("found '{}' implement class '{}' from kin.factories", extensionClassName, implementClassName);
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
    private static void checkSupport(Class<?> extensionClass) {
        if (!extensionClass.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException(extensionClass.getCanonicalName().concat("doesn't support spi, please ensure @SPI"));
        }
    }

    /**
     * 获取{@link ExtensionLoader}实例
     */
    @SuppressWarnings("unchecked")
    private static <E> ExtensionLoader<E> getExtensionLoader(ClassLoader classLoader, Class<E> extensionClass) {
        checkSupport(extensionClass);

        //尝试获取
        Map<Class<?>, ExtensionLoader<?>> class2ExtensionLoader = classloader2Class2ExtensionLoader.get(classLoader);
        if (Objects.nonNull(class2ExtensionLoader)) {
            ExtensionLoader<?> extensionLoader = class2ExtensionLoader.get(extensionClass);
            if (Objects.nonNull(extensionLoader)) {
                //已构建, 则直接返回
                return (ExtensionLoader<E>) extensionLoader;
            }
        }

        //还未构建, 则初始化
        synchronized (ExtensionLoader.class) {
            //copy
            Map<ClassLoader, Map<Class<?>, ExtensionLoader<?>>> classloader2Class2ExtensionLoader = new HashMap<>(ExtensionLoader.classloader2Class2ExtensionLoader);
            class2ExtensionLoader = classloader2Class2ExtensionLoader.get(classLoader);
            if (Objects.isNull(class2ExtensionLoader)) {
                //没有第一层map, 则创建
                class2ExtensionLoader = new HashMap<>(4);
                classloader2Class2ExtensionLoader.put(classLoader, class2ExtensionLoader);
            }

            ExtensionLoader<?> extensionLoader = class2ExtensionLoader.get(extensionClass);
            if (Objects.nonNull(extensionLoader)) {
                //已构建, 则直接返回
                return (ExtensionLoader<E>) extensionLoader;
            }

            //初始化
            extensionLoader = new ExtensionLoader<>(classLoader, extensionClass);
            class2ExtensionLoader.put(extensionClass, extensionLoader);

            //更新值
            ExtensionLoader.classloader2Class2ExtensionLoader = Collections.unmodifiableMap(classloader2Class2ExtensionLoader);
            return (ExtensionLoader<E>) extensionLoader;
        }
    }

    /**
     * 使用默认{@link ClassLoader}获取{@link ExtensionLoader}实例
     */
    private static <E> ExtensionLoader<E> getExtensionLoader(Class<E> extensionClass) {
        return getExtensionLoader(ExtensionLoader.class.getClassLoader(), extensionClass);
    }

    /**
     * 根据extension class name | extension class simple name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     *
     * @param extensionClass extension class
     * @param name           extension class name | extension class simple name | {@link Extension#value()}
     */
    public static <E> E getExtension(Class<E> extensionClass, String name) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByName(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据extension class name | extension class simple name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     *
     * @param extensionClass extension class
     * @param name           extension class name | extension class simple name | {@link Extension#value()}
     * @param args           extension class实现类构造方法参数
     */
    public static <E> E getExtension(Class<E> extensionClass, String name, Object... args) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByName(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 根据extension class name | extension class simple name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     * 如果找不到, 则返回默认的extension实现类, 即name为{@link SPI#value()}的extension实现类
     *
     * @param extensionClass extension class
     * @param name           extension class name | extension class simple name | {@link Extension#value()}
     */
    public static <E> E getExtensionOrDefault(Class<E> extensionClass, String name) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByNameOrDefault(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据extension class name | extension class simple name | {@link Extension#value()}获取extension实现类实例
     * 忽略大小写
     * 如果找不到, 则返回默认的extension实现类, 即name为{@link SPI#value()}的extension实现类
     *
     * @param extensionClass extension class
     * @param name           extension class name | extension class simple name | {@link Extension#value()}
     * @param args           extension class实现类构造方法参数
     */
    public static <E> E getExtensionOrDefault(Class<E> extensionClass, String name, Object... args) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByNameOrDefault(name);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     *
     * @param extensionClass extension class
     * @param code           对应实现类{@link Extension#code()}
     */
    public static <E> E getExtension(Class<E> extensionClass, int code) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByCode((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     *
     * @param extensionClass extension class
     * @param code           对应实现类{@link Extension#code()}
     * @param args           extension class实现类构造方法参数
     */
    public static <E> E getExtension(Class<E> extensionClass, int code, Object... args) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByCode((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     * 如果找不到, 则返回默认的extension实现类, 即name为{@link SPI#value()}的extension实现类
     *
     * @param extensionClass extension class
     * @param code           对应实现类{@link Extension#code()}
     */
    public static <E> E getExtensionOrDefault(Class<E> extensionClass, int code) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByCodeOrDefault((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance();
        }
        return null;
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类实例
     * 如果找不到, 则返回默认的extension实现类, 即name为{@link SPI#value()}的extension实现类
     *
     * @param extensionClass extension class
     * @param code           对应实现类{@link Extension#code()}
     * @param args           extension class实现类构造方法参数
     */
    public static <E> E getExtensionOrDefault(Class<E> extensionClass, int code, Object... args) {
        ExtensionMetaData<E> extensionMetaData = getExtensionLoader(extensionClass).getByCodeOrDefault((byte) code);
        if (Objects.nonNull(extensionMetaData)) {
            return extensionMetaData.getInstance(args);
        }
        return null;
    }

    /**
     * 获取所有extension class实现类实例
     *
     * @return 列表是以 {@link Extension#order()}降序排序
     */
    public static <E> List<E> getExtensions(Class<E> extensionClass) {
        //使用默认构造器创建extension实现类实例
        return getExtensionLoader(extensionClass).getSortedExtensionMetaDataList().stream().map(ExtensionMetaData::getInstance).collect(Collectors.toList());
    }
    //-------------------------------------------------------------------------------------------------------------------

    /**
     * 从{@code dir}/{extension class}加载extension class及其实现类定义
     */
    private Set<String> loadFromServiceFile(String dir) {
        Set<String> implClassNames = new HashSet<>();
        String fileName = dir + File.separator + extensionClass.getName();
        try {
            Enumeration<URL> props = classLoader.getResources(fileName);
            //遍历所有dir/{extension class}文件
            while (props.hasMoreElements()) {
                URL url = props.nextElement();
                try (InputStream inputStream = url.openStream()) {
                    parseServiceFile(inputStream, implClassNames);
                }
            }
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
        return implClassNames;
    }

    /**
     * 解析以extension class命名的文件
     */
    private void parseServiceFile(InputStream inputStream, Set<String> implClassNames) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                //逐行解析
                if (StringUtils.isBlank(line)) {
                    //空行
                    continue;
                }

                line = line.trim();
                int i0 = line.indexOf('#');
                if (i0 == 0 || line.length() == 0) {
                    //注释 or 空行
                    continue;
                }

                int equalIndex = line.indexOf("=");
                if (equalIndex > 0) {
                    //spring.factories格式, 即key-value格式, 也就是properties
                    String extensionClassName = line.substring(0, equalIndex).trim();

                    if (!extensionClass.getName().equals(extensionClassName) && !getAlias().equals(extensionClassName)) {
                        //过滤非指定extension class的class定义
                        continue;
                    }

                    String implementClassNamesStr = line.substring(equalIndex + 1).trim();
                    List<String> implementClassNames = Arrays.asList(implementClassNamesStr.split(","));
                    implClassNames.addAll(implementClassNames);

                    if (log.isDebugEnabled()) {
                        for (String implementClassName : implementClassNames) {
                            log.debug("found '{}' implement class '{}' from service file", extensionClassName, implementClassName);
                        }
                    }
                } else {
                    //java spi格式, 即每一行都是一个实现类
                    String extensionClassName = extensionClass.getName();
                    implClassNames.add(line);

                    if (log.isDebugEnabled()) {
                        log.debug("found '{}' implement class '{}' from service file", extensionClassName, line);
                    }
                }
            }

        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 从kin.factories获取指定extension class的实现类
     */
    private Set<String> getImplClassNamesFromFactories() {
        //1. extension class name
        Set<String> implClassNamesFromFactories = new HashSet<>(getImplClassNamesFromFactories(extensionClass.getName()));

        //2. @SPI注解定义的alias
        String alias = spi.alias();
        if (StringUtils.isNotBlank(alias)) {
            implClassNamesFromFactories.addAll(getImplClassNamesFromFactories(alias));
        }

        return implClassNamesFromFactories;
    }

    /**
     * 初始化所有加载到的extension class的实现类, 并创建{@link ExtensionMetaData}缓存
     *
     * @param implClassNames         加载到的extension class的实现类名
     * @param extensionMetaDataList  {@link #getExtensionLoader(Class)}
     * @param name2ExtensionMetaData {@link #getName2ExtensionMetaData()}
     * @param code2ExtensionMetaData {@link #getCode2ExtensionMetaData()}
     */
    @SuppressWarnings("unchecked")
    private void initAllExtensionMetaData(Set<String> implClassNames,
                                          List<ExtensionMetaData<E>> extensionMetaDataList,
                                          Map<String, ExtensionMetaData<E>> name2ExtensionMetaData,
                                          @Nullable Map<Byte, ExtensionMetaData<E>> code2ExtensionMetaData) {
        for (String implClassName : implClassNames) {
            Class<? extends E> implClass;
            try {
                implClass = (Class<? extends E>) Class.forName(implClassName, false, classLoader);
            } catch (ClassNotFoundException e) {
                ExceptionUtils.throwExt(e);
                return;
            }

            //检查实现类是否继承(实现)extension class
            if (!extensionClass.isAssignableFrom(implClass)) {
                throw new IllegalArgumentException(
                        String.format("fail to load extension '%s', because it is not subtype of '%s'",
                                implClass.getCanonicalName(), extensionClass.getName()));
            }

            //获取Extension注解
            Extension extension = implClass.getAnnotation(Extension.class);
            if (isCoded() && extension.code() < 0) {
                //如果开启了coded, 但是Extension没有配置code, 则抛异常
                throw new IllegalArgumentException(
                        String.format("fail to load extension '%s', because it's code of @Extension must >=0",
                                implClass.getCanonicalName()));
            }

            //构建ExtensionMetaData实例
            ExtensionMetaData<E> extensionMetaData = new ExtensionMetaData<>(implClass, isSingleton());
            extensionMetaDataList.add(extensionMetaData);

            //可用names
            Set<String> names = new HashSet<>(4);
            //Extension#value()
            String extensionAlias = extensionMetaData.getAlias();
            if (StringUtils.isNotBlank(extensionAlias)) {
                names.add(extensionAlias.toLowerCase());
            }
            //extension class simple name
            names.add(implClass.getSimpleName().toLowerCase());
            //extension canonical class name
            names.add(implClass.getCanonicalName().toLowerCase());
            //extension class name
            names.add(implClass.getName().toLowerCase());

            for (String name : names) {
                ExtensionMetaData<E> oldExtensionMetaData = name2ExtensionMetaData.put(name, extensionMetaData);
                //检查name是否会冲突
                if (Objects.nonNull(oldExtensionMetaData)) {
                    throw new IllegalArgumentException(
                            String.format("fail to load extension '%s', because it's name is conflict, name=%s, conflict extension class is '%s'",
                                    implClass.getCanonicalName(), name, oldExtensionMetaData.getExtensionClass().getCanonicalName()));
                }
            }

            if (isCoded()) {
                //如果开启了coded, 则需要缓存到code2ExtensionMetaData
                byte code = extensionMetaData.getCode();
                //noinspection ConstantConditions
                ExtensionMetaData<E> oldExtensionMetaData = code2ExtensionMetaData.put(code, extensionMetaData);
                //检查code是否会冲突
                if (Objects.nonNull(oldExtensionMetaData)) {
                    throw new IllegalArgumentException(
                            String.format("fail to load extension '%s', because it's code of @Extension is conflict, code=%d, conflict extension class is '%s'",
                                    implClass.getCanonicalName(), code, oldExtensionMetaData.getExtensionClass().getCanonicalName()));
                }
            }
        }

        //根据@Extension#order()降序排序
        Comparator<ExtensionMetaData<E>> comparator = Comparator.comparingInt(ExtensionMetaData::getOrder);
        extensionMetaDataList.sort(comparator.reversed());
    }

    /**
     * 获取默认extension class实现类
     */
    @Nullable
    public ExtensionMetaData<E> getDefaultExtension() {
        return getByName(getDefaultExtensionName());
    }

    /**
     * 根据extension class name | extension class simple name | {@link Extension#value()}获取extension实现类元数据
     * 忽略大小写
     *
     * @param name extension class name | extension class simple name | {@link Extension#value()}
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
        //extension class simple name
        availableNames.add(extensionClass.getPackage().getName().concat(".").concat(name).toLowerCase());
        //extension class name
        availableNames.add(extensionClass.getPackage().getName().concat(".").concat(name).concat(extensionClass.getSimpleName()).toLowerCase());

        for (String availableName : availableNames) {
            ExtensionMetaData<E> defaultExtensionMetaData = name2ExtensionMetaData.get(availableName);
            if (Objects.nonNull(defaultExtensionMetaData)) {
                return defaultExtensionMetaData;
            }
        }

        return null;
    }

    /**
     * 根据extension class name | extension class simple name | {@link Extension#value()}获取extension实现类元数据
     * 忽略大小写
     * 如果找不到, 则返回默认的extension实现类, 即name为{@link SPI#value()}的extension实现类
     *
     * @param name extension class name | extension class simple name | {@link Extension#value()}
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
     *
     * @param code 对应实现类{@link Extension#code()}
     */
    @Nullable
    public ExtensionMetaData<E> getByCode(byte code) {
        return code2ExtensionMetaData.get(code);
    }

    /**
     * 根据{@link Extension#code()}获取extension实现类元数据
     * 如果找不到, 则返回默认的extension实现类, 即name为{@link SPI#value()}的extension实现类
     *
     * @param code 对应实现类{@link Extension#code()}
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
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Class<E> getExtensionClass() {
        return extensionClass;
    }

    public SPI getSpi() {
        return spi;
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

    public List<ExtensionMetaData<E>> getSortedExtensionMetaDataList() {
        return sortedExtensionMetaDataList;
    }

    public Set<String> getImplClassNames() {
        return implClassNames;
    }

    public Map<String, ExtensionMetaData<E>> getName2ExtensionMetaData() {
        return name2ExtensionMetaData;
    }

    public Map<Byte, ExtensionMetaData<E>> getCode2ExtensionMetaData() {
        return code2ExtensionMetaData;
    }

    //-------------------------------------------------------------------------------------------------------------------------------------

    /**
     * 配置的且{@link Extension}标识的extension class元数据
     */
    private static final class ExtensionMetaData<E> implements Comparable<ExtensionMetaData<E>> {
        /** extension implement class */
        private final Class<? extends E> claxx;
        /** 是否单例 */
        private final boolean singleton;
        /**
         * extension implement class定义的{@link Extension}
         * 可能为null
         */
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
