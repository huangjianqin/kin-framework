package org.kin.framework.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * java 句柄工具类
 *
 * @author huangjianqin
 * @date 2021/3/8
 */
public final class MethodHandleUtils {
    private MethodHandleUtils() {
    }

    /** interface default method handles */
    private static final Map<Method, MethodHandle> INTERFACE_DEFAULT_METHOD_HANDLE_MAP = new ConcurrentHashMap<>();

    /** common method handles */
    private static final Map<Method, MethodHandle> METHOD_HANDLE_MAP = new ConcurrentHashMap<>();

    /**
     * 获取接口默认方法句柄, 直接调用接口默认方法的逻辑, 而不是实现类覆盖的逻辑
     */
    public static MethodHandle getInterfaceDefaultMethodHandle(Method method, Class<?> interfaceClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!method.isDefault() || !interfaceClass.isInterface() || !interfaceClass.isAssignableFrom(method.getDeclaringClass())) {
            throw new IllegalArgumentException("class must be interface and its method is default method");
        }
        MethodHandle methodHandle = INTERFACE_DEFAULT_METHOD_HANDLE_MAP.get(method);
        if (methodHandle == null) {
            String version = System.getProperty("java.version");
            if (version.startsWith("1.8.")) {
                //java 1.8+
                Constructor<MethodHandles.Lookup> lookupConstructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                if (!lookupConstructor.isAccessible()) {
                    lookupConstructor.setAccessible(true);
                }
                methodHandle = lookupConstructor.newInstance(method.getDeclaringClass(), MethodHandles.Lookup.PRIVATE)
                        .unreflectSpecial(method, method.getDeclaringClass());
            } else {
                methodHandle = MethodHandles.lookup().findSpecial(
                        method.getDeclaringClass(),
                        method.getName(),
                        MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
                        interfaceClass);
            }
            INTERFACE_DEFAULT_METHOD_HANDLE_MAP.put(method, methodHandle);
        }
        return methodHandle;
    }

    /**
     * 调用接口默认方法句柄
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeInterfaceDefaultMethodHandle(Method method, Class<?> interfaceClass, Object instance, Object... args) throws Throwable {
        MethodHandle methodHandle = getInterfaceDefaultMethodHandle(method, interfaceClass);
        return (T) methodHandle.bindTo(instance).invokeWithArguments(args);
    }

    /**
     * 获取普通方法句柄
     */
    public static MethodHandle getCommonMethodHandle(Method method) throws NoSuchMethodException, IllegalAccessException {
        MethodHandle methodHandle = METHOD_HANDLE_MAP.get(method);
        if (methodHandle == null) {
            MethodType methodType = MethodType.methodType(method.getReturnType(), method.getParameterTypes());
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            methodHandle = lookup.findVirtual(method.getDeclaringClass(), method.getName(), methodType);

            METHOD_HANDLE_MAP.put(method, methodHandle);
        }
        return methodHandle;
    }

    /**
     * 调用普通方法句柄
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeCommonMethodHandle(Method method, Object instance, Object... args) throws Throwable {
        MethodHandle methodHandle = getCommonMethodHandle(method);
        return (T) methodHandle.bindTo(instance).invokeWithArguments(args);
    }
}
