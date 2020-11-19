package org.kin.framework.utils;

import com.google.common.collect.Sets;
import org.kin.framework.collection.Tuple;

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
            throw new RuntimeException(e);
        }
    }

    public static <T> T instance(String classStr) {
        if (StringUtils.isBlank(classStr)) {
            return null;
        }
        try {
            Class<T> claxx = (Class<T>) Class.forName(classStr);
            return instance(claxx);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据参数调用构造器实例化类
     */
    public static <T> T instance(Class<T> claxx, Object... args) {
        if (claxx == null) {
            return null;
        }
        try {
            List<Class> argClasses = new ArrayList<>();
            for (Object arg : args) {
                argClasses.add(arg.getClass());
            }
            Constructor<T> constructor = claxx.getDeclaredConstructor(argClasses.toArray(new Class[1]));
            return constructor.newInstance(args);
        } catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Class getClass(String className) {
        if (StringUtils.isBlank(className)) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取某个类的所有子类, 但不包括该类
     */
    public static <T> Set<Class<? extends T>> getSubClass(String packageName, Class<T> parent, boolean isIncludeJar) {
        return scanClasspathAndFindMatch(packageName, parent,
                (c, target) -> !c.equals(target) && c.isAssignableFrom(target), isIncludeJar);
    }

    /**
     * 获取出现某注解的所有类, 包括抽象类和接口
     */
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
                                        throw new RuntimeException(e);
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
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return subClasses;
    }

    public static <T> T getFieldValue(Object target, String fieldName) {
        for (Field field : getAllFields(target.getClass())) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                try {
                    return (T) field.get(target);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } finally {
                    field.setAccessible(false);
                }
            }
        }

        return null;
    }

    public static void setFieldValue(Object target, String fieldName, Object newValue) {
        for (Field field : getAllFields(target.getClass())) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                try {
                    field.set(target, newValue);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
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
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取getter方法
     */
    public static Method getterMethod(Class<?> target, Field field) {
        byte[] items = field.getName().getBytes();
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        try {
            if (isBoolean(field.getType())) {
                //如果是boolean, 先尝试is开头的getter方法, 找不到再尝试get开头
                Method getterMethod = target.getMethod("is" + new String(items));
                if (getterMethod != null) {
                    return getterMethod;
                }
            }
            return target.getMethod("get" + new String(items));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method setterMethod(Class<?> target, Field field) {
        byte[] items = field.getName().getBytes();
        items[0] = (byte) ((char) items[0] - 'a' + 'A');
        try {
            return target.getMethod("set" + new String(items), field.getType());
        } catch (NoSuchMethodException e) {
            return null;
        }
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

    public static List<Class<?>> getAllClasses(Class<?> claxx) {
        return getClasses(claxx, Object.class);
    }

    /**
     * 获取claxx -> parent的所有class
     */
    public static List<Class<?>> getClasses(Class<?> claxx, Class<?> parent) {
        if (parent.isAssignableFrom(claxx)) {
            throw new IllegalStateException(String.format("%s is not super class of %s", parent.getName(), claxx.getName()));
        }
        List<Class<?>> classes = new ArrayList<>();
        while (!claxx.equals(parent)) {
            classes.add(claxx);
            claxx = claxx.getSuperclass();
        }
        return classes;
    }

    /**
     * 获取默认值
     */
    public static Object getDefaultValue(Class claxx) {
        if (claxx.isPrimitive()) {
            if (String.class.equals(claxx)) {
                return "";
            } else if (Boolean.class.equals(claxx) || Boolean.TYPE.equals(claxx)) {
                return false;
            } else if (Byte.class.equals(claxx) || Byte.TYPE.equals(claxx)) {
                return 0;
            } else if (Character.class.equals(claxx) || Character.TYPE.equals(claxx)) {
                return "";
            } else if (Short.class.equals(claxx) || Short.TYPE.equals(claxx)) {
                return 0;
            } else if (Integer.class.equals(claxx) || Integer.TYPE.equals(claxx)) {
                return 0;
            } else if (Long.class.equals(claxx) || Long.TYPE.equals(claxx)) {
                return 0L;
            } else if (Float.class.equals(claxx) || Float.TYPE.equals(claxx)) {
                return 0.0F;
            } else if (Double.class.equals(claxx) || Double.TYPE.equals(claxx)) {
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
    public static boolean isInterfaceAnnotationPresent(Object o, Class annotation) {
        Class claxx = o.getClass();
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
                throw new RuntimeException("illeagal data type, type=" + fieldType);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 继承的父类
     * 获取类泛型具体实现类型
     * 由于类型擦除, 获取不了本类的泛型类型参数具体类型, 但其保存的父类的泛型类型参数具体类型, 所以可以获取父类的泛型类型参数具体类型
     */
    public static List<Class<?>> getSuperClassGenericActualTypes(Class<?> claxx) {
        List<Class<?>> result = new ArrayList<>();

        Type genericSuperclass = claxx.getGenericSuperclass();
        for (Type actualTypeArgument : ((ParameterizedType) genericSuperclass).getActualTypeArguments()) {
            if (actualTypeArgument instanceof ParameterizedType) {
                result.add((Class<?>) ((ParameterizedType) actualTypeArgument).getRawType());
            } else {
                result.add((Class<?>) actualTypeArgument);
            }
        }

        return result;
    }

    /**
     * 实现的接口
     * 获取类泛型具体实现类型
     * 由于类型擦除, 获取不了本类的泛型类型参数具体类型, 但其保存的父类的泛型类型参数具体类型, 所以可以获取父类的泛型类型参数具体类型
     */
    public static List<Class<?>> getSuperInterfacesGenericActualTypes(Class<?> claxx) {
        List<Class<?>> result = new ArrayList<>();

        Type[] genericInterfaces = claxx.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            for (Type actualTypeArgument : ((ParameterizedType) genericInterface).getActualTypeArguments()) {
                if (actualTypeArgument instanceof ParameterizedType) {
                    result.add((Class<?>) ((ParameterizedType) actualTypeArgument).getRawType());
                } else {
                    result.add((Class<?>) actualTypeArgument);
                }
            }
        }


        return result;
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

    //------------------------------------------------------------字节码相关------------------------------------------------------------

    /**
     * 基础类型封箱
     */
    public static String primitivePackage(Class claxx, String code) {
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
    public static String primitiveUnpackage(Class claxx, String code) {
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
        sb.append("$");

        StringJoiner paramsJoiner = new StringJoiner("$");
        Class[] paramTypes = method.getParameterTypes();
        for (Class paramType : paramTypes) {
            paramsJoiner.add(paramType.getTypeName());
        }
        sb.append(paramsJoiner.toString());

        sb.append("$");
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
            } else {
                argSJ.add(Object.class.getName() + " " + METHOD_DECLARATION_ARG_NAME + i);
            }
        }
        sb.append(argSJ.toString());
        sb.append(")");

        StringJoiner throwsSJ = new StringJoiner(", ");
        Class[] exceptionTypes = method.getExceptionTypes();
        for (Class exceptionType : exceptionTypes) {
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
}