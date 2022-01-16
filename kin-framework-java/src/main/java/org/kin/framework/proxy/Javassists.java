package org.kin.framework.proxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import org.kin.framework.utils.ExceptionUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * javassist增强工具类
 *
 * @author huangjianqin
 * @date 2020/12/24
 */
public class Javassists {
    /**
     * POOL.get()方法必须使用的是{@link Class#getName()}, 不能是{@link Class#getCanonicalName()}
     */
    private static final ClassPool POOL = ClassPool.getDefault();
    /** ctclass缓存 */
    private static final Multimap<String, CtClass> CTCLASS_CACHE = HashMultimap.create();

    private Javassists() {
    }

    /**
     * 缓存对应的已编译好的CtClass
     */
    public static void cacheCTClass(String className, CtClass ctClass) {
        CTCLASS_CACHE.put(className, ctClass);
    }

    /**
     * @return ClassPool
     */
    public static ClassPool getPool() {
        return POOL;
    }

    /**
     * 尝试释放${@link ClassPool}无用空间
     */
    public static void detach(String className) {
        if (CTCLASS_CACHE.containsKey(className)) {
            for (CtClass ctClass : CTCLASS_CACHE.get(className)) {
                ctClass.detach();
            }
            CTCLASS_CACHE.removeAll(className);
        }
    }

    /**
     * 将方法参数类型转换成CtClass[]
     */
    public static CtClass[] getParamCtClasses(ClassPool classPool, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        CtClass[] parameterCtClass = new CtClass[parameterTypes.length];

        for (int i = 0; i < parameterCtClass.length; i++) {
            try {
                parameterCtClass[i] = classPool.get(parameterTypes[i].getName());
            } catch (NotFoundException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        return parameterCtClass;
    }

    /**
     * 生成CtMethod
     */
    public static CtMethod convertCtMethod(ClassPool classPool, Method method, String methodBody, int modifier, CtClass declaring) {
        Preconditions.checkArgument(modifier > 0, "method modifier must be greater than zero");

        StringBuilder sb = new StringBuilder();
        sb.append("{").append(System.lineSeparator());
        sb.append(methodBody);
        sb.append("}").append(System.lineSeparator());

        try {
            CtMethod ctMethod = new CtMethod(classPool.get(method.getReturnType().getName()), method.getName(), getParamCtClasses(classPool, method), declaring);
            ctMethod.setModifiers(modifier);
            ctMethod.setBody(sb.toString());
            return ctMethod;
        } catch (NotFoundException | CannotCompileException e) {
            ExceptionUtils.throwExt(e);
        }

        return null;
    }

    /**
     * 生成CtMethod, 并添加至声明CtClass中
     */
    public static void makeCtMethod(ClassPool classPool, Method method, String methodBody, int modifier, CtClass declaring) {
        CtMethod ctMethod = convertCtMethod(classPool, method, methodBody, modifier, declaring);
        if (Objects.isNull(ctMethod)) {
            //理论上不会到这里
            throw new IllegalStateException("encounter unknown error");
        }
        try {
            declaring.addMethod(ctMethod);
        } catch (CannotCompileException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 生成public final CtMethod, 并添加至声明CtClass中
     */
    public static void makeCtPublicFinalMethod(ClassPool classPool, Method method, String methodBody, CtClass declaring) {
        makeCtMethod(classPool, method, methodBody, Modifier.PUBLIC + Modifier.FINAL, declaring);
    }

}
