package org.kin.framework.utils;

import com.google.common.collect.Sets;
import org.kin.framework.collection.Tuple;
import org.springframework.util.Assert;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;
import sun.reflect.generics.reflectiveObjects.TypeVariableImpl;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by huangjianqin on 2018/1/26.
 */
public class ClassUtils {
    public static final String CLASS_SUFFIX = ".class";
    /** 用于匹配内部类 */
    private static final Pattern INNER_PATTERN = Pattern.compile("\\$(\\d+).", Pattern.CASE_INSENSITIVE);
    /** 生成方法签名的参数命名 */
    public static final String METHOD_DECLARATION_ARG_NAME = "arg";
    /** key -> 泛型类型, value -> 第一个泛型类型参数 */
    private static final Map<Type, Class<?>> GENERIC_TYPES_CACHE = new ConcurrentHashMap<>();
    /** key -> 基础类型, value -> 包装类 */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_TYPE_MAP = new IdentityHashMap<>(8);
    /** key -> 包装类, value -> 基础类型 */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPE_TO_WRAPPER_MAP = new IdentityHashMap<>(8);

    static {
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Character.class, char.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Double.class, double.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Float.class, float.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Integer.class, int.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Long.class, long.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Short.class, short.class);
        PRIMITIVE_WRAPPER_TYPE_MAP.put(Void.class, void.class);

        for (Map.Entry<Class<?>, Class<?>> entry : PRIMITIVE_WRAPPER_TYPE_MAP.entrySet()) {
            PRIMITIVE_TYPE_TO_WRAPPER_MAP.put(entry.getValue(), entry.getKey());
        }
    }

    @FunctionalInterface
    private interface Matcher<T> {
        /**
         * @param c      基准
         * @param target 目标
         * @return true表示匹配
         */
        boolean match(Class<? extends T> c, Class<? extends T> target);
    }

    /**
     * 通过无参构造器实例化类
     */
    public static <T> T instance(Class<T> claxx) {
        if (claxx == null) {
            return null;
        }
        try {
            return claxx.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ExceptionUtils.throwExt(e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T instance(String classStr) {
        if (StringUtils.isBlank(classStr)) {
            return null;
        }
        try {
            Class<T> claxx = (Class<T>) Class.forName(classStr);
            return instance(claxx);
        } catch (ClassNotFoundException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 根据参数调用构造器实例化类
     */
    public static <T> T instance(Class<T> claxx, Object... args) {
        if (claxx == null) {
            return null;
        }

        if (CollectionUtils.isEmpty(args)) {
            return instance(claxx);
        }

        try {
            Class<?>[] argClasses = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argClasses[i] = args[i].getClass();
            }
            Constructor<T> constructor = claxx.getDeclaredConstructor(argClasses);
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    public static Class<?> getClass(String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 获取某个类的所有子类, 但不包括该类
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<Class<? extends T>> getSubClass(String packageName, Class<T> parent, boolean isIncludeJar) {
        return scanClasspathAndFindMatch(packageName, parent,
                (c, target) -> !c.equals(target) && c.isAssignableFrom(target), isIncludeJar);
    }

    /**
     * 获取出现某注解的所有类, 包括抽象类和接口
     */
    @SuppressWarnings("unchecked")
    public static <T> Set<Class<? extends T>> getAnnotationedClass(String packageName, Class<T> annotationClass, boolean isIncludeJar) {
        if (annotationClass.isAnnotation()) {
            return scanClasspathAndFindMatch(packageName, annotationClass,
                    (c, target) -> {
                        if (target.isAnnotationPresent(c)) {
                            return true;
                        } else {
                            for (Field field : target.getDeclaredFields()) {
                                if (field.isAnnotationPresent(c)) {
                                    return true;
                                }
                            }

                            for (Method method : target.getDeclaredMethods()) {
                                if (method.isAnnotationPresent(c)) {
                                    return true;
                                }
                            }
                        }

                        return false;
                    }, isIncludeJar);
        }
        return Collections.emptySet();
    }

    @SuppressWarnings("rawtypes")
    public static <T> Set<Class<? extends T>> scanClasspathAndFindMatch(String packageName, Class<T> c, Matcher matcher, boolean isIncludeJar) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader staticClassLoader = ClassUtils.class.getClassLoader();
        ClassLoader[] classLoaders = contextClassLoader != null ?
                (staticClassLoader != null && contextClassLoader != staticClassLoader ?
                        new ClassLoader[]{contextClassLoader, staticClassLoader} : new ClassLoader[]{contextClassLoader})
                : new ClassLoader[0];

        Set<Class<? extends T>> subClasses = Sets.newLinkedHashSet();
        for (ClassLoader classLoader : classLoaders) {
            subClasses.addAll(scanClasspathAndFindMatch(classLoader, packageName, c, matcher, isIncludeJar));
        }

        return subClasses;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> Set<Class<T>> scanClasspathAndFindMatch(ClassLoader contextClassLoader, String packageName, Class<T> c, Matcher matcher, boolean isIncludeJar) {
        Set<Class<T>> subClasses = Sets.newLinkedHashSet();

        String packageResource = packageName.replaceAll("\\.", "/");
        try {
            Enumeration<URL> urls = contextClassLoader.getResources(packageResource);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if ("file".equals(url.getProtocol())) {
                    Path path = Paths.get(url.toURI());
                    Stream<Path> stream = Files.walk(path);
                    Set<Class<T>> classes = stream.filter(p -> !Files.isDirectory(p) && p.toString().endsWith(CLASS_SUFFIX))
                            .map(p -> {
                                URI uri = p.toUri();
                                String origin = uri.getPath();
                                //空包名要去掉root路径, 不然会解析出来的包名会包含部分classpath路径, 也就是无效了
                                //如果指定包名, 下面lastIndexOf(packageName)会过滤掉部分classpath路径
                                String className;
                                if (StringUtils.isBlank(packageName)) {
                                    origin = origin.substring(origin.indexOf(url.getPath()) + url.getPath().length());
                                    int endIndex = origin.lastIndexOf(CLASS_SUFFIX);
                                    className = origin.substring(0, endIndex);
                                    //把/替换成.
                                    className = className.replaceAll("/", ".");
                                } else {
                                    //把/替换成.
                                    origin = origin.replaceAll("/", ".");
                                    int startIndex = origin.lastIndexOf(packageName);
                                    int endIndex = origin.lastIndexOf(CLASS_SUFFIX);
                                    className = origin.substring(startIndex, endIndex);
                                }

                                if (StringUtils.isNotBlank(className) &&
                                        !INNER_PATTERN.matcher(className).find() &&
                                        (className.indexOf("$") <= 0)) {
                                    try {
                                        return (Class<T>) contextClassLoader.loadClass(className);
                                    } catch (ClassNotFoundException e) {
                                        ExceptionUtils.throwExt(e);
                                    }
                                }
                                return null;
                            })
                            .filter(claxx -> !Objects.isNull(claxx))
                            .filter(claxx -> matcher.match(c, claxx)).collect(Collectors.toSet());
                    subClasses.addAll(Sets.newHashSet(classes));
                } else if ("jar".equals(url.getProtocol()) && isIncludeJar) {
                    JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarURLConnection.getJarFile();
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String entryName = jarEntry.getName();

                        if (jarEntry.isDirectory()) {
                            continue;
                        }

                        if (entryName.endsWith("/") || !entryName.endsWith(CLASS_SUFFIX)) {
                            continue;
                        }

                        if (INNER_PATTERN.matcher(entryName).find() || entryName.indexOf("$") > 0) {
                            continue;
                        }

                        String className = entryName.replaceAll("/", ".");
                        className = className.substring(0, entryName.lastIndexOf(".class"));

                        try {
                            Class<T> claxx = (Class<T>) contextClassLoader.loadClass(className);
                            if (matcher.match(c, claxx)) {
                                subClasses.add(claxx);
                            }
                        } catch (ClassNotFoundException e) {
                            ExceptionUtils.throwExt(e);
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            ExceptionUtils.throwExt(e);
        }

        return subClasses;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Object target, String fieldName) {
        for (Field field : getAllFields(target.getClass())) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                try {
                    return (T) field.get(target);
                } catch (IllegalAccessException e) {
                    ExceptionUtils.throwExt(e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }

        throw new IllegalStateException("encounter unknown error");
    }

    public static void setFieldValue(Object target, String fieldName, Object newValue) {
        for (Field field : getAllFields(target.getClass())) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                try {
                    field.set(target, newValue);
                } catch (IllegalAccessException e) {
                    ExceptionUtils.throwExt(e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }
    }

    /**
     * 通过getter field实例设置值
     */
    public static Object getFieldValue(Object instance, Field field) {
        try {
            Method m = getterMethod(instance.getClass(), field);
            if (m != null) {
                return m.invoke(instance);
            } else {
                try {
                    field.setAccessible(true);
                    return field.get(instance);
                } finally {
                    field.setAccessible(false);
                }
            }

        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 通过setter field实例设置值
     */
    public static void setFieldValue(Object instance, Field field, Object value) {
        try {
            Method m = setterMethod(instance.getClass(), field);
            if (m != null) {
                m.invoke(instance, value);
            } else {
                try {
                    field.setAccessible(true);
                    field.set(instance, value);
                } finally {
                    field.setAccessible(false);
                }
            }
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 获取getter方法
     */
    public static Method getterMethod(Class<?> target, Field field) {
        try {
            String s = StringUtils.firstUpperCase(field.getName());
            if (isBoolean(field.getType())) {
                //如果是boolean, 先尝试is开头的getter方法, 找不到再尝试get开头
                return target.getMethod("is".concat(s));
            }
            return target.getMethod("get".concat(s));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 获取getter方法
     */
    public static Method getterMethod(Field field) {
        return getterMethod(field.getDeclaringClass(), field);
    }

    /**
     * 获取setter方法
     */
    public static Method setterMethod(Class<?> target, Field field) {
        try {
            return target.getMethod("set".concat(StringUtils.firstUpperCase(field.getName())), field.getType());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 获取setter方法
     */
    public static Method setterMethod(Field field) {
        return setterMethod(field.getDeclaringClass(), field);
    }

    public static List<Field> getAllFields(Class<?> claxx) {
        return getFields(claxx, Object.class);
    }

    /**
     * 获取claxx -> parent的所有field
     * field顺序, 子类 -> 父类 -> 父父类
     */
    public static List<Field> getFields(Class<?> claxx, Class<?> parent) {
        if (claxx == null || parent == null) {
            return Collections.emptyList();
        }

        List<Field> fields = new ArrayList<>();
        while (!claxx.equals(parent)) {
            Collections.addAll(fields, claxx.getDeclaredFields());
            claxx = claxx.getSuperclass();
        }
        return fields;
    }

    public static List<Method> getAllMethods(Class<?> claxx) {
        return getMethods(claxx, Object.class);
    }

    /**
     * 获取claxx -> parent的所有method
     * method顺序, 子类 -> 父类 -> 父父类
     */
    public static List<Method> getMethods(Class<?> claxx, Class<?> parent) {
        if (claxx == null || parent == null) {
            return Collections.emptyList();
        }

        List<Method> methods = new ArrayList<>();
        while (!claxx.equals(parent)) {
            Collections.addAll(methods, claxx.getDeclaredMethods());
            claxx = claxx.getSuperclass();
        }
        return methods;
    }

    public static List<Class<?>> getAllClasses(Class<?> claxx) {
        return getClasses(claxx, Object.class);
    }

    /**
     * 获取claxx -> parent的所有class
     */
    public static List<Class<?>> getClasses(Class<?> claxx, Class<?> parent) {
        if (!parent.isAssignableFrom(claxx)) {
            throw new IllegalStateException(String.format("%s is not super class of %s", parent.getName(), claxx.getName()));
        }
        List<Class<?>> classes = new ArrayList<>();
        while (Objects.nonNull(claxx) && !claxx.equals(parent)) {
            classes.add(claxx);
            claxx = claxx.getSuperclass();
        }
        return classes;
    }

    /**
     * 获取默认值
     */
    public static Object getDefaultValue(Class<?> claxx) {
        if (claxx.isPrimitive()) {
            if (Boolean.TYPE.equals(claxx)) {
                return false;
            } else if (Byte.TYPE.equals(claxx)) {
                return 0;
            } else if (Character.TYPE.equals(claxx)) {
                return "";
            } else if (Short.TYPE.equals(claxx)) {
                return 0;
            } else if (Integer.TYPE.equals(claxx)) {
                return 0;
            } else if (Long.TYPE.equals(claxx)) {
                return 0L;
            } else if (Float.TYPE.equals(claxx)) {
                return 0.0F;
            } else if (Double.TYPE.equals(claxx)) {
                return 0.0D;
            }
        } else {
            if (String.class.equals(claxx)) {
                return "";
            } else if (Boolean.class.equals(claxx)) {
                return false;
            } else if (Byte.class.equals(claxx)) {
                return 0;
            } else if (Character.class.equals(claxx)) {
                return "";
            } else if (Short.class.equals(claxx)) {
                return 0;
            } else if (Integer.class.equals(claxx)) {
                return 0;
            } else if (Long.class.equals(claxx)) {
                return 0L;
            } else if (Float.class.equals(claxx)) {
                return 0.0F;
            } else if (Double.class.equals(claxx)) {
                return 0.0D;
            }
        }
        return null;
    }

    /**
     * bytes -> 基础类型
     */
    public static <T> T convertBytes2PrimitiveObj(Class<T> claxx, byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }

        String strValue = new String(bytes);
        return convertStr2PrimitiveObj(claxx, strValue);
    }

    /**
     * string -> 基础类型
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertStr2PrimitiveObj(Class<T> claxx, String strValue) {
        if (StringUtils.isNotBlank(strValue)) {
            if (String.class.equals(claxx)) {
                return (T) strValue;
            } else if (Boolean.class.equals(claxx) || Boolean.TYPE.equals(claxx)) {
                return (T) Boolean.valueOf(strValue);
            } else if (Byte.class.equals(claxx) || Byte.TYPE.equals(claxx)) {
                return (T) Byte.valueOf(strValue);
            } else if (Character.class.equals(claxx) || Character.TYPE.equals(claxx)) {
                return (T) strValue;
            } else if (Short.class.equals(claxx) || Short.TYPE.equals(claxx)) {
                return (T) Short.valueOf(strValue);
            } else if (Integer.class.equals(claxx) || Integer.TYPE.equals(claxx)) {
                return (T) Integer.valueOf(strValue);
            } else if (Long.class.equals(claxx) || Long.TYPE.equals(claxx)) {
                return (T) Long.valueOf(strValue);
            } else if (Float.class.equals(claxx) || Float.TYPE.equals(claxx)) {
                return (T) Float.valueOf(strValue);
            } else if (Double.class.equals(claxx) || Double.TYPE.equals(claxx)) {
                return (T) Double.valueOf(strValue);
            }
        }

        return null;
    }

    /**
     * @return 该类实现的接口是否有指定注解标识
     */
    public static boolean isInterfaceAnnotationPresent(Object o, Class<?> annotation) {
        Class<?> claxx = o.getClass();
        while (claxx != null) {
            for (Class interfaceClass : claxx.getInterfaces()) {
                if (interfaceClass.isAnnotationPresent(annotation)) {
                    return true;
                }
            }
            claxx = claxx.getSuperclass();
        }

        return false;
    }

    /**
     * string -> 基础类型
     */
    public static Object string2Obj(Field field, String value) {
        Class<?> fieldType = field.getType();

        if (StringUtils.isBlank(value)) {
            return null;
        }

        value = value.trim();

        try {
            if (String.class.equals(fieldType)) {
                return value;
            } else if (Boolean.class.equals(fieldType) || Boolean.TYPE.equals(fieldType)) {
                return Boolean.valueOf(value);
            } else if (Byte.class.equals(fieldType) || Byte.TYPE.equals(fieldType)) {
                return Byte.valueOf(value);
            } else if (Character.class.equals(fieldType) || Character.TYPE.equals(fieldType)) {
                return value.toCharArray()[0];
            } else if (Short.class.equals(fieldType) || Short.TYPE.equals(fieldType)) {
                return Short.valueOf(value);
            } else if (Integer.class.equals(fieldType) || Integer.TYPE.equals(fieldType)) {
                return Integer.valueOf(value);
            } else if (Long.class.equals(fieldType) || Long.TYPE.equals(fieldType)) {
                return Long.valueOf(value);
            } else if (Float.class.equals(fieldType) || Float.TYPE.equals(fieldType)) {
                return Float.valueOf(value);
            } else if (Double.class.equals(fieldType) || Double.TYPE.equals(fieldType)) {
                return Double.valueOf(value);
            } else {
                throw new IllegalArgumentException("illeagal data type, type=" + fieldType);
            }
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("encounter unknown error");
    }

    /**
     * 继承的父类
     * 获取类泛型具体实现类型
     * 由于类型擦除, 获取不了本类的泛型类型参数具体类型, 但其保存的父类的泛型类型参数具体类型, 所以可以获取父类的泛型类型参数具体类型
     */
    public static List<Type> getSuperClassGenericActualTypes(Class<?> claxx) {
        Type genericSuperclass = claxx.getGenericSuperclass();
        return new ArrayList<>(Arrays.asList(((ParameterizedType) genericSuperclass).getActualTypeArguments()));
    }

    /**
     * 继承的父类
     * 获取类泛型raw type(如果该类也是带泛型的, 则会丢掉泛型信息)
     */
    public static List<Class<?>> getSuperClassGenericRawTypes(Class<?> claxx) {
        return getSuperClassGenericActualTypes(claxx).stream().map(type -> {
            if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            } else {
                return ((Class<?>) type);
            }
        }).collect(Collectors.toList());
    }

    /**
     * 实现的接口
     * 获取指定接口的泛型具体实现类型
     * 由于类型擦除, 获取不了本类的泛型类型参数具体类型, 但其保存的父接口的泛型类型参数具体类型, 所以可以获取父接口的泛型类型参数具体类型
     *
     * @param claxx          接口实现类
     * @param interfaceClass 指定接口
     */
    public static List<Type> getSuperInterfacesGenericActualTypes(Class<?> interfaceClass, Class<?> claxx) {
        if (!interfaceClass.isAssignableFrom(claxx)) {
            //非实现类
            return Collections.emptyList();
        }

        //临时缓存父类泛型参数名字以及类型的对应关系
        Map<String, Type> paramName2Type = new HashMap<>();
        Type target = claxx;
        while (!Object.class.equals(target)) {
            Type[] genericInterfaces;
            if (target instanceof Class) {
                genericInterfaces = ((Class<?>) target).getGenericInterfaces();
                target = ((Class<?>) target).getGenericSuperclass();
            } else if (target instanceof ParameterizedType) {
                // 只知道直接父类的泛型参数类型数据, 其父类或者实现接口的泛型类型是不知道的
                // 所以这里需要提前获取直接父类的泛型参数名字及其类型的对应关系
                ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) target;
                genericInterfaces = parameterizedType.getRawType().getGenericInterfaces();

                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                TypeVariable<? extends Class<?>>[] typeParameters = parameterizedType.getRawType().getTypeParameters();
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable<? extends Class<?>> typeVariable = typeParameters[i];
                    paramName2Type.put(typeVariable.getName(), actualTypeArguments[i]);
                }
                target = parameterizedType.getRawType().getGenericSuperclass();
            } else {
                throw new IllegalStateException("unhandle statment");
            }
            for (Type genericInterface : genericInterfaces) {
                if (!(genericInterface instanceof ParameterizedType)) {
                    continue;
                }
                ParameterizedType parameterizedInterface = (ParameterizedType) genericInterface;
                if (!parameterizedInterface.getRawType().equals(interfaceClass)) {
                    continue;
                }
                //找到对应的interfaceClass
                List<Type> result = new ArrayList<>();
                for (Type actualTypeArgument : parameterizedInterface.getActualTypeArguments()) {
                    if (actualTypeArgument instanceof TypeVariableImpl) {
                        //父类中, 根据泛型参数名字获取对应的类型
                        TypeVariableImpl<?> typeVariable = (TypeVariableImpl<?>) actualTypeArgument;
                        String paramName = typeVariable.getName();
                        if (paramName2Type.containsKey(paramName)) {
                            result.add(paramName2Type.get(paramName));
                        }
                    } else {
                        result.add(actualTypeArgument);
                    }
                }

                return result;
            }
        }


        return Collections.emptyList();
    }

    /**
     * 实现的接口
     * 获取指定接口的泛型raw type(如果该类也是带泛型的, 则会丢掉泛型信息)
     *
     * @param claxx          接口实现类
     * @param interfaceClass 指定接口
     */
    public static List<Class<?>> getSuperInterfacesGenericRawTypes(Class<?> interfaceClass, Class<?> claxx) {
        return getSuperInterfacesGenericActualTypes(interfaceClass, claxx).stream().map(type -> {
            if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            } else {
                return ((Class<?>) type);
            }
        }).collect(Collectors.toList());
    }

    /**
     * @return 是否是boolean
     */
    public static boolean isBoolean(Class<?> target) {
        return Boolean.class.equals(target) || Boolean.TYPE.equals(target);
    }

    /**
     * @param field 数组 | 集合类 成员变量
     * @return 元素类型
     */
    public static Class<?> getItemType(Field field) {
        Class<?> fieldType = field.getType();
        if (fieldType.isArray()) {
            //数组
            return fieldType.getComponentType();
        } else if (Collection.class.isAssignableFrom(fieldType)) {
            //集合
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            return (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }

        throw new UnsupportedOperationException();
    }

    /**
     * @param field map 成员变量
     * @return key value 类型
     */
    public static Tuple<Class<?>, Class<?>> getKVType(Field field) {
        if (!Map.class.isAssignableFrom(field.getType())) {
            throw new UnsupportedOperationException();
        }
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        return new Tuple<>((Class<?>) parameterizedType.getActualTypeArguments()[0], (Class<?>) parameterizedType.getActualTypeArguments()[1]);
    }

    /**
     * 判断是否可以rhsType是可以被assigned to lhsType, 考虑基础类型
     */
    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        Assert.notNull(lhsType, "Left-hand side type must not be null");
        Assert.notNull(rhsType, "Right-hand side type must not be null");
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }
        if (lhsType.isPrimitive()) {
            Class<?> resolvedPrimitive = PRIMITIVE_WRAPPER_TYPE_MAP.get(rhsType);
            return (lhsType == resolvedPrimitive);
        } else {
            Class<?> resolvedWrapper = PRIMITIVE_TYPE_TO_WRAPPER_MAP.get(rhsType);
            return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
        }
    }

    //------------------------------------------------------------字节码相关------------------------------------------------------------

    /**
     * 基础类型封箱
     */
    public static String primitivePackage(Class<?> claxx, String code) {
        StringBuilder sb = new StringBuilder();
        // 需要手动装箱, 不然编译会报错
        if (claxx.isPrimitive()) {
            if (Integer.TYPE.equals(claxx)) {
                sb.append("Integer.valueOf(");
            } else if (Short.TYPE.equals(claxx)) {
                sb.append("Short.valueOf(");
            } else if (Byte.TYPE.equals(claxx)) {
                sb.append("Byte.valueOf(");
            } else if (Long.TYPE.equals(claxx)) {
                sb.append("Long.valueOf(");
            } else if (Float.TYPE.equals(claxx)) {
                sb.append("Float.valueOf(");
            } else if (Double.TYPE.equals(claxx)) {
                sb.append("Double.valueOf(");
            } else if (Character.TYPE.equals(claxx)) {
                sb.append("Character.valueOf(");
            }
        }
        sb.append(code);
        if (claxx.isPrimitive() && !Void.TYPE.equals(claxx)) {
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * 基础类型拆箱
     */
    public static String primitiveUnpackage(Class<?> claxx, String code) {
        //需要手动拆箱, 不然编译会报错
        if (Integer.TYPE.equals(claxx)) {
            return "((" + Integer.class.getSimpleName() + ")" + code + ").intValue()";
        } else if (Short.TYPE.equals(claxx)) {
            return "((" + Short.class.getSimpleName() + ")" + code + ").shortValue()";
        } else if (Byte.TYPE.equals(claxx)) {
            return "((" + Byte.class.getSimpleName() + ")" + code + ").byteValue()";
        } else if (Long.TYPE.equals(claxx)) {
            return "((" + Long.class.getSimpleName() + ")" + code + ").longValue()";
        } else if (Float.TYPE.equals(claxx)) {
            return "((" + Float.class.getSimpleName() + ")" + code + ").floatValue()";
        } else if (Double.TYPE.equals(claxx)) {
            return "((" + Double.class.getSimpleName() + ")" + code + ").doubleValue()";
        } else if (Character.TYPE.equals(claxx)) {
            return "((" + Character.class.getSimpleName() + ")" + code + ").charValue()";
        } else if (!Void.TYPE.equals(claxx)) {
            return "(" + claxx.getName() + ")" + code;
        }
        return code;
    }

    /**
     * 获取{@link Method}唯一标识
     */
    public static String getUniqueName(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName());

        Class<?>[] paramTypes = method.getParameterTypes();
        if (CollectionUtils.isNonEmpty(paramTypes)) {
            sb.append("$");
        }
        StringJoiner paramsJoiner = new StringJoiner("$");
        for (Class<?> paramType : paramTypes) {
            paramsJoiner.add(paramType.getTypeName());
        }
        sb.append(paramsJoiner);
        return sb.toString();
    }

    /**
     * 生成方法签名(编译后的, 也就是类型擦除, 解语法糖后), 方法参数从 ${ClassUtils.METHOD_DECLARATION_ARG_NAME}0, ${ClassUtils.METHOD_DECLARATION_ARG_NAME}1, ${ClassUtils.METHOD_DECLARATION_ARG_NAME}2....
     */
    public static String generateMethodDeclaration(Method method) {
        if (method == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Modifier.toString(method.getModifiers())).append(" ");
        sb.append(method.getReturnType().getName()).append(" ");
        sb.append(method.getName()).append("(");

        StringJoiner argSJ = new StringJoiner(", ");
        Type[] paramTypes = method.getGenericParameterTypes();
        for (int i = 0; i < paramTypes.length; i++) {
            String paramTypeStr = paramTypes[i].getTypeName();
//            if (method.isVarArgs() && (i == paramTypes.length - 1)) {
//                param = param.replaceFirst("\\[\\]$", "...");
//            }
            //处理范性类型擦除
            if (paramTypes[i] instanceof Class) {
                argSJ.add(paramTypeStr + " " + METHOD_DECLARATION_ARG_NAME + i);
            } else if (paramTypes[i] instanceof ParameterizedType) {
                argSJ.add(((ParameterizedType) paramTypes[i]).getRawType().getTypeName() + " " + METHOD_DECLARATION_ARG_NAME + i);
            } else {
                argSJ.add(Object.class.getName() + " " + METHOD_DECLARATION_ARG_NAME + i);
            }
        }
        sb.append(argSJ.toString());
        sb.append(")");

        StringJoiner throwsSJ = new StringJoiner(", ");
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (Class<?> exceptionType : exceptionTypes) {
            throwsSJ.add(exceptionType.getName());
        }
        if (throwsSJ.length() > 0) {
            sb.append(" throws ");
            sb.append(throwsSJ.toString());
        }

        String methodDeclarationStr = sb.toString();
        methodDeclarationStr = methodDeclarationStr.replace("abstract ", "");
        methodDeclarationStr = methodDeclarationStr.replace("transient ", "");
        methodDeclarationStr = methodDeclarationStr.replace("native ", "");
        return methodDeclarationStr;
    }

    /**
     * @param className 类名
     * @return 判断一个类是否存在
     */
    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成方法
     */
    public static String generateMethodContent(Method method, String methodBody) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateMethodDeclaration(method));
        sb.append("{").append(System.lineSeparator());
        sb.append(methodBody).append(System.lineSeparator());
        sb.append("}");
        return sb.toString();
    }

    /**
     * 父类为XXX
     * 实现类为{prefixName}XXXX
     * 获取{prefixName}, 全小写
     *
     * @param targetClass 实现类
     * @param baseClass   父类
     */
    public static <T> String getPrefixName(Class<? extends T> targetClass, Class<T> baseClass) {
        String baseName = baseClass.getSimpleName();
        String targetName = targetClass.getSimpleName();

        int index = targetName.indexOf(baseName);
        if (index > 0) {
            return targetName.substring(0, index).toLowerCase();
        }
        return null;
    }

    /**
     * 根据genericType获取泛型类型, 只支持泛型类型只有一个的情况
     * get inferred class for generic type, such as Flux like, please refer http://tutorials.jenkov.com/java-reflection/generics.html
     * <p>
     * 支持缓存
     *
     * @param genericType generic type
     * @return inferred class
     */
    public static Class<?> getInferredClassForGeneric(Type genericType) {
        //performance promotion by cache
        if (!GENERIC_TYPES_CACHE.containsKey(genericType)) {
            try {
                Class<?> inferredClass = parseInferredClass(genericType);
                if (inferredClass != null) {
                    GENERIC_TYPES_CACHE.put(genericType, inferredClass);
                } else {
                    GENERIC_TYPES_CACHE.put(genericType, Object.class);
                }
            } catch (Exception e) {
                return Object.class;
            }
        }
        return GENERIC_TYPES_CACHE.get(genericType);
    }

    /**
     * get inferred class for Generic Type, please refer http://tutorials.jenkov.com/java-reflection/generics.html
     *
     * @param genericType generic type
     * @return inferred class
     */
    public static Class<?> parseInferredClass(Type genericType) {
        Class<?> inferredClass = null;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0) {
                final Type typeArgument = typeArguments[0];
                if (typeArgument instanceof ParameterizedType) {
                    inferredClass = (Class<?>) ((ParameterizedType) typeArgument).getActualTypeArguments()[0];
                } else if (typeArgument instanceof Class) {
                    inferredClass = (Class<?>) typeArgument;
                } else {
                    String typeName = typeArgument.getTypeName();
                    if (typeName.contains(" ")) {
                        typeName = typeName.substring(typeName.lastIndexOf(" ") + 1);
                    }
                    if (typeName.contains("<")) {
                        typeName = typeName.substring(0, typeName.indexOf("<"));
                    }
                    try {
                        inferredClass = Class.forName(typeName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (inferredClass == null && genericType instanceof Class) {
            inferredClass = (Class<?>) genericType;
        }
        return inferredClass;
    }
}
