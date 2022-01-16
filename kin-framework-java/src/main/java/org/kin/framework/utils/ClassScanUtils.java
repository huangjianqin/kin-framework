package org.kin.framework.utils;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * class扫描工具类
 *
 * @author huangjianqin
 * @date 2021/7/20
 */
public final class ClassScanUtils {
    private ClassScanUtils() {
    }

    /**
     * 扫描所有类,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并执行指定class处理逻辑
     *
     * @param claxx    接口 | 注解 | 父类
     * @param consumer 扫描到的class的处理逻辑
     */
    public static <T> void scan(Class<T> claxx, Consumer<Class<?>> consumer) {
        scan("*", claxx, consumer);
    }

    /**
     * 扫描指定package下,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并执行{@code consumer}逻辑
     *
     * @param pkg      包名
     * @param claxx    接口 | 注解 | 父类
     * @param consumer 扫描到的class的处理逻辑
     */
    public static <T> void scan(String pkg, Class<T> claxx, Consumer<Class<?>> consumer) {
        ClassGraph classGraph = new ClassGraph();
        if (claxx.isAnnotation()) {
            //注解
            classGraph.enableAnnotationInfo();
        }

        try (ScanResult scanResult = classGraph
                .enableClassInfo()
                .acceptPackages(pkg)
                .scan()) {
            getClassInfoList(claxx, scanResult).forEach(ci -> consumer.accept(ci.loadClass()));
        }
    }

    /**
     * 扫描所有类,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并返回
     *
     * @param claxx 接口 | 注解 | 父类
     */
    public static <T> List<Class<?>> scan(Class<T> claxx) {
        return scan("*", claxx);
    }

    /**
     * 扫描指定package下,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并返回
     *
     * @param pkg   包名
     * @param claxx 接口 | 注解 | 父类
     */
    public static <T> List<Class<?>> scan(String pkg, Class<T> claxx) {
        ClassGraph classGraph = new ClassGraph();
        if (claxx.isAnnotation()) {
            //注解
            classGraph.enableAnnotationInfo();
        }

        try (ScanResult scanResult = classGraph
                .enableClassInfo()
                .acceptPackages(pkg)
                .scan()) {
            return getClassInfoList(claxx, scanResult).stream().map(ClassInfo::loadClass).collect(Collectors.toList());
        }
    }

    private static <T> ClassInfoList getClassInfoList(Class<T> claxx, ScanResult scanResult) {
        String className = claxx.getName();
        ClassInfoList classInfoList;
        if (claxx.isAnnotation()) {
            //注解
            classInfoList = scanResult.getClassesWithAnnotation(className);
        } else if (claxx.isInterface()) {
            //接口实现类
            classInfoList = scanResult.getClassesImplementing(className);
        } else {
            //继承
            classInfoList = scanResult.getSubclasses(className);
        }
        return classInfoList;
    }

    /**
     * 异步扫描指定package下,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并执行{@code consumer}逻辑
     *
     * @param claxx    接口 | 注解 | 父类
     * @param consumer 扫描到的class的处理逻辑
     */
    public static <T> void scanAsync(Class<T> claxx, Consumer<Class<?>> consumer) {
        scanAsync("*", claxx, consumer);
    }

    /**
     * 异步扫描所有类,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并执行{@code consumer}逻辑
     *
     * @param pkg      包名
     * @param claxx    接口 | 注解 | 父类
     * @param consumer 扫描到的class的处理逻辑
     */
    public static <T> void scanAsync(String pkg, Class<T> claxx, Consumer<Class<?>> consumer) {
        scanAsync(pkg, claxx, consumer, System.err::println);
    }

    /**
     * 异步扫描所有类,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并执行{@code consumer}逻辑
     *
     * @param claxx    接口 | 注解 | 父类
     * @param consumer 扫描到的class的处理逻辑
     */
    public static <T> void scanAsync(Class<T> claxx, Consumer<Class<?>> consumer, ClassGraph.FailureHandler failureHandler) {
        scanAsync("*", claxx, consumer, failureHandler);
    }

    /**
     * 异步扫描指定package下,
     * 1. 指定实现类
     * 2. 带注解的类
     * 3. 指定父类继承子类
     * 并执行{@code consumer}逻辑
     *
     * @param pkg      包名
     * @param claxx    接口 | 注解 | 父类
     * @param consumer 扫描到的class的处理逻辑
     */
    public static <T> void scanAsync(String pkg, Class<T> claxx, Consumer<Class<?>> consumer, ClassGraph.FailureHandler failureHandler) {
        ClassGraph classGraph = new ClassGraph();
        if (claxx.isAnnotation()) {
            //注解
            classGraph.enableAnnotationInfo();
        }

        classGraph.enableClassInfo()
                .acceptPackages(pkg)
                .scanAsync(ForkJoinPool.commonPool(), SysUtils.getSuitableThreadNum(), sr -> {
                    ClassInfoList classInfoList = getClassInfoList(claxx, sr);
                    classInfoList.forEach(ci -> consumer.accept(ci.loadClass()));
                }, failureHandler);
    }
}
