package org.kin.framework.proxy.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import org.kin.framework.proxy.MethodBodyConstructor;
import org.kin.framework.proxy.ProxyDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.ProxyMethodDefinition;
import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 利用javassist字节码技术增加代理类 调用速度更快
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public class ProxyEnhanceUtils {
    private static final Logger log = LoggerFactory.getLogger(ProxyEnhanceUtils.class);
    private static final ClassPool POOL = ClassPool.getDefault();
    private static Multimap<String, CtClass> CTCLASS_CACHE = HashMultimap.create();

    public static final String DEFAULT_PROXY_FIELD_NAME = "proxy";

    private ProxyEnhanceUtils() {
    }

    /**
     * 为了ProxyInvoker的invoker方法生成代理方法调用代码
     */
    private static String generateProxyInvokerInvokeCode(String fieldName, Method proxyMethod) {
        StringBuffer invokeCode = new StringBuffer();

        Class<?> returnType = proxyMethod.getReturnType();
        if (!returnType.equals(Void.TYPE)) {
            invokeCode.append("result = ");
        }

        StringBuffer oneLineCode = new StringBuffer();
        oneLineCode.append(fieldName.concat(".").concat(proxyMethod.getName()).concat("("));

        Class[] paramTypes = proxyMethod.getParameterTypes();
        StringJoiner paramBody = new StringJoiner(", ");
        for (int i = 0; i < paramTypes.length; i++) {
            //因为ProxyInvoker的invoker方法只有一个参数, Object[], 所以从param0取方法
            paramBody.add(org.kin.framework.utils.ClassUtils.primitiveUnpackage(paramTypes[i], ClassUtils.METHOD_DECLARATION_ARG_NAME + "0[" + i + "]"));
        }

        oneLineCode.append(paramBody.toString());
        oneLineCode.append(")");

        invokeCode.append(org.kin.framework.utils.ClassUtils.primitivePackage(returnType, oneLineCode.toString()));
        invokeCode.append(";");

        return invokeCode.toString();
    }

    private static CtClass generateEnhanceMethodProxyClass(Class<?> proxyObjClass, Method proxyMethod, String proxyCtClassName) {
        CtClass proxyCtClass = POOL.makeClass(proxyCtClassName);
        try {
            //实现接口
            proxyCtClass.addInterface(POOL.getCtClass(ProxyInvoker.class.getName()));

            //添加成员域
            String prxoyFieldName = DEFAULT_PROXY_FIELD_NAME;
            CtField proxyCtField = new CtField(POOL.get(proxyObjClass.getName()), prxoyFieldName, proxyCtClass);
            proxyCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyCtField);

            String methodFieldName = "method";
            CtField methodCtField = new CtField(POOL.get(Method.class.getName()), methodFieldName, proxyCtClass);
            methodCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(methodCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{POOL.get(proxyObjClass.getName()), POOL.get(Method.class.getName())}, proxyCtClass);
            ctConstructor.setBody("{$0.".concat(prxoyFieldName).concat(" = $1;").concat("$0.").concat(methodFieldName).concat(" = $2;}"));
            proxyCtClass.addConstructor(ctConstructor);

            //方法体
            //invoke
            Method invokeMethod = ProxyInvoker.class.getMethod("invoke", Object[].class);
            StringBuffer methodBody = new StringBuffer();
            methodBody.append(ClassUtils.generateMethodDeclaration(invokeMethod).concat("{"));
            methodBody.append("Object result = null;");
            methodBody.append(generateProxyInvokerInvokeCode(prxoyFieldName, proxyMethod));
            methodBody.append("return result; }");

            CtMethod invokeCtMethod = CtMethod.make(methodBody.toString(), proxyCtClass);
            proxyCtClass.addMethod(invokeCtMethod);

            //getProxyObj
            Method getProxyObjMethod = ProxyInvoker.class.getMethod("getProxyObj");
            methodBody = new StringBuffer();
            methodBody.append(ClassUtils.generateMethodDeclaration(getProxyObjMethod).concat("{"));
            methodBody.append("return ".concat(prxoyFieldName).concat("; }"));

            CtMethod getProxyObjCtMethod = CtMethod.make(methodBody.toString(), proxyCtClass);
            proxyCtClass.addMethod(getProxyObjCtMethod);

            //getMethod
            Method getMethodMethod = ProxyInvoker.class.getMethod("getMethod");
            methodBody = new StringBuffer();
            methodBody.append(ClassUtils.generateMethodDeclaration(getMethodMethod).concat("{"));
            methodBody.append("return ".concat(methodFieldName).concat("; }"));

            CtMethod getMethodCtMethod = CtMethod.make(methodBody.toString(), proxyCtClass);
            proxyCtClass.addMethod(getMethodCtMethod);

            cacheCTClass(proxyCtClassName, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            log.error(proxyMethod.toString(), e);
        }

        return null;
    }

    /**
     * 增强某个方法代理的调用
     */
    public static ProxyInvoker enhanceMethod(ProxyMethodDefinition definition) {
        Object proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();
        Method proxyMethod = definition.getMethod();
        String proxyCtClassName = definition.getClassName();

        Class realProxyClass = null;
        try {
            realProxyClass = Class.forName(proxyCtClassName);
        } catch (ClassNotFoundException e) {

        }

        if (Objects.isNull(realProxyClass)) {
            CtClass proxyCtClass = POOL.getOrNull(proxyCtClassName);
            if (proxyCtClass == null) {
                proxyCtClass = generateEnhanceMethodProxyClass(proxyObjClass, proxyMethod, proxyCtClassName);
            }

            if (proxyCtClass != null) {
                try {
                    realProxyClass = proxyCtClass.toClass();
                } catch (Exception e) {
                    log.error(proxyMethod.toString(), e);
                }
            }
        }

        if (Objects.nonNull(realProxyClass)) {
            try {
                return (ProxyInvoker) realProxyClass.getConstructor(proxyObjClass, Method.class).newInstance(proxyObj, proxyMethod);
            } catch (Exception e) {
                log.error(proxyMethod.toString(), e);
            }
        }

        return null;
    }
    //---------------------------------------------------------------------------------------------------------------------------------

    /**
     * 为目标方法生成代理方法调用代码
     */
    public static String generateProxyInvokeCode(String proxyFieldName, Method proxyMethod) {
        StringBuffer methodBody = new StringBuffer();

        if (!proxyMethod.getReturnType().equals(Void.TYPE)) {
            methodBody.append("return ");
        }

        StringBuffer oneLineCode = new StringBuffer();
        oneLineCode.append(proxyFieldName.concat(".").concat(proxyMethod.getName()).concat("("));

        Class[] paramTypes = proxyMethod.getParameterTypes();
        StringJoiner paramBody = new StringJoiner(", ");
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].isPrimitive()) {
                paramBody.add(ClassUtils.METHOD_DECLARATION_ARG_NAME + i);
            } else {
                paramBody.add(ClassUtils.primitivePackage(paramTypes[i], ClassUtils.METHOD_DECLARATION_ARG_NAME + i));
            }
        }

        oneLineCode.append(paramBody.toString());
        oneLineCode.append(")");

        Class<?> returnType = proxyMethod.getReturnType();
        if (returnType.isPrimitive()) {
            methodBody.append(oneLineCode.toString());
        } else {
            methodBody.append(ClassUtils.primitivePackage(proxyMethod.getReturnType(), oneLineCode.toString()));
        }
        methodBody.append(";");

        return methodBody.toString();
    }

    /**
     * @param proxyClass 需要代理的类, 即是需要实现(继承)的类
     */
    private static CtClass generateEnhanceClassProxyClass(Class<?> proxyClass, String className, MethodBodyConstructor methodBodyConstructor) {
        CtClass proxyCtClass = POOL.makeClass(className);
        try {
            if (proxyClass.isInterface()) {
                //实现接口
                proxyCtClass.addInterface(POOL.getCtClass(proxyClass.getName()));
            } else {
                //继承类
                proxyCtClass.setSuperclass(POOL.getCtClass(proxyClass.getName()));
            }

            //添加成员域
            String prxoyFieldName = DEFAULT_PROXY_FIELD_NAME;
            CtField proxyCtField = new CtField(POOL.get(proxyClass.getName()), prxoyFieldName, proxyCtClass);
            proxyCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{POOL.get(proxyClass.getName())}, proxyCtClass);
            ctConstructor.setBody("{$0.".concat(prxoyFieldName).concat(" = $1;}"));
            proxyCtClass.addConstructor(ctConstructor);

            //类实现
            //invoke
            for (Method method : proxyClass.getMethods()) {
                if (Modifier.isFinal(method.getModifiers())) {
                    continue;
                }
                StringBuilder methodCode = new StringBuilder();
                methodCode.append(ClassUtils.generateMethodDeclaration(method).concat("{"));
                methodCode.append(methodBodyConstructor.construct(prxoyFieldName, method));
                methodCode.append(" }");

                CtMethod ctMethod = CtMethod.make(methodCode.toString(), proxyCtClass);
                proxyCtClass.addMethod(ctMethod);
            }

            cacheCTClass(className, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            log.error(proxyClass.toString(), e);
        }

        return null;
    }

    /**
     * @param proxyObjClass 需要代理的类或该类的某一接口
     */
    private static <P> P enhanceClass0(
            Object proxyObj,
            Class<?> proxyObjClass,
            String packageName,
            String proxyCtClassName,
            MethodBodyConstructor methodBodyConstructor) {
        Class realProxyClass = null;
        try {
            realProxyClass = Class.forName(proxyCtClassName);
        } catch (ClassNotFoundException e) {

        }

        if (Objects.isNull(realProxyClass)) {
            CtClass proxyCtClass = POOL.getOrNull(proxyCtClassName);
            if (proxyCtClass == null) {
                proxyCtClass = generateEnhanceClassProxyClass(proxyObjClass, packageName, methodBodyConstructor);
                if (Objects.nonNull(proxyCtClass)) {
                    try {
                        realProxyClass = proxyCtClass.toClass();
                    } catch (Exception e) {
                        log.error(proxyObjClass.toString(), e);
                    }
                }
            }
        }

        if (Objects.nonNull(realProxyClass)) {
            try {
                return (P) realProxyClass.getConstructor(proxyObjClass).newInstance(proxyObj);
            } catch (Exception e) {
                log.error(proxyObjClass.toString(), e);
            }
        }

        return null;
    }

    public static <P> P enhanceClass(ProxyDefinition definition) {
        Object proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();
        String packageName = definition.getPackageName();
        String proxyCtClassName = packageName.concat(".").concat(proxyObjClass.getSimpleName()).concat("$JavassistProxy");

        return enhanceClass0(proxyObj, proxyObjClass, packageName, proxyCtClassName, definition.getMethodBodyConstructor());
    }

    public static <P> P enhanceClass(ProxyDefinition definition, Class<P> interfaceClass) {
        Object proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();

        if (!interfaceClass.isAssignableFrom(proxyObjClass)) {
            return null;
        }

        String packageName = definition.getPackageName();
        String proxyCtClassName = packageName.concat(".").concat(interfaceClass.getSimpleName()).concat("$JavassistProxy");

        return enhanceClass0(proxyObj, interfaceClass, packageName, proxyCtClassName, definition.getMethodBodyConstructor());
    }

    //---------------------------------------------------------------------------------------------------------------------------------

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

    public static void cacheCTClass(String className, CtClass ctClass) {
        CTCLASS_CACHE.put(className, ctClass);
    }


    public static ClassPool getPool() {
        return POOL;
    }

    //---------------------------------------------------------------------------------------------------------------------------------
}
