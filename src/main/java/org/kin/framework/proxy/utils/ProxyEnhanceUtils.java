package org.kin.framework.proxy.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import org.kin.framework.proxy.ProxyDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.kin.framework.proxy.ProxyMethodDefinition;
import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * 利用javassist字节码技术增加代理类 调用速度更快
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public class ProxyEnhanceUtils {
    private static final Logger log = LoggerFactory.getLogger(ProxyEnhanceUtils.class);
    private static final ClassPool pool = ClassPool.getDefault();
    private static Multimap<String, CtClass> CTCLASS_CACHE = HashMultimap.create();

    /**
     * {@link ProxyInvoker} 的方法名改变, 这里也需要更改
     */
    private static final String INVOKE_METHOD_SIGNATURE = "public Object invoke(Object[] params) throws Exception";
    private static final String GETPROXYOBj_METHOD_SIGNATURE = "public S getProxyObj()";
    private static final String GETMETHOD_METHOD_SIGNATURE = "public " + Method.class.getName() + " getMethod()";

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
        oneLineCode.append(fieldName + "." + proxyMethod.getName() + "(");

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
        CtClass proxyCtClass = pool.makeClass(proxyCtClassName);
        try {
            //实现接口
            proxyCtClass.addInterface(pool.getCtClass(ProxyInvoker.class.getName()));

            //添加成员域
            String prxoyFieldName = "proxy";
            CtField proxyCtField = new CtField(pool.get(proxyObjClass.getName()), prxoyFieldName, proxyCtClass);
            proxyCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyCtField);

            String methodFieldName = "method";
            CtField methodCtField = new CtField(pool.get(Method.class.getName()), methodFieldName, proxyCtClass);
            methodCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(methodCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.get(proxyObjClass.getName()), pool.get(Method.class.getName())}, proxyCtClass);
            ctConstructor.setBody("{$0." + prxoyFieldName + " = $1;" + "$0." + methodFieldName + " = $2;}");
            proxyCtClass.addConstructor(ctConstructor);

            //方法体
            //invoke
            Method invokeMethod = ProxyInvoker.class.getMethod("invoke", Object[].class);
            StringBuffer methodBody = new StringBuffer();
            methodBody.append(ClassUtils.generateMethodDeclaration(invokeMethod) + "{");
            methodBody.append("Object result = null;");
            methodBody.append(generateProxyInvokerInvokeCode(prxoyFieldName, proxyMethod));
            methodBody.append("return result; }");

            CtMethod invokeCtMethod = CtMethod.make(methodBody.toString(), proxyCtClass);
            proxyCtClass.addMethod(invokeCtMethod);

            //getProxyObj
            Method getProxyObjMethod = ProxyInvoker.class.getMethod("getProxyObj");
            methodBody = new StringBuffer();
            methodBody.append(ClassUtils.generateMethodDeclaration(getProxyObjMethod) + "{");
            methodBody.append("return " + prxoyFieldName + "; }");

            CtMethod getProxyObjCtMethod = CtMethod.make(methodBody.toString(), proxyCtClass);
            proxyCtClass.addMethod(getProxyObjCtMethod);

            //getMethod
            Method getMethodMethod = ProxyInvoker.class.getMethod("getMethod");
            methodBody = new StringBuffer();
            methodBody.append(ClassUtils.generateMethodDeclaration(getMethodMethod) + "{");
            methodBody.append("return " + methodFieldName + "; }");

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

        CtClass proxyCtClass = pool.getOrNull(proxyCtClassName);
        if (proxyCtClass == null) {
            proxyCtClass = generateEnhanceMethodProxyClass(proxyObjClass, proxyMethod, proxyCtClassName);
        }

        if (proxyCtClass != null) {
            try {
                return (ProxyInvoker) proxyCtClass.toClass().getConstructor(proxyObjClass, Method.class).newInstance(proxyObj, proxyMethod);
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
    private static String generateProxyInvokeCode(String fieldName, Method proxyMethod) {
        StringBuffer methodBody = new StringBuffer();

        StringBuffer oneLineCode = new StringBuffer();
        oneLineCode.append(fieldName + "." + proxyMethod.getName() + "(");

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

    private static CtClass generateEnhanceClassProxyClass(Class<?> proxyObjClass, String className) {
        CtClass proxyCtClass = pool.makeClass(className);
        try {
            //实现接口
            proxyCtClass.setSuperclass(pool.getCtClass(proxyObjClass.getName()));

            //添加成员域
            String prxoyFieldName = "proxy";
            CtField proxyCtField = new CtField(pool.get(proxyObjClass.getName()), prxoyFieldName, proxyCtClass);
            proxyCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{pool.get(proxyObjClass.getName())}, proxyCtClass);
            ctConstructor.setBody("{$0." + prxoyFieldName + " = $1;}");
            proxyCtClass.addConstructor(ctConstructor);

            //类实现
            //invoke
            for (Method method : proxyObjClass.getMethods()) {
                if (Modifier.isFinal(method.getModifiers())) {
                    continue;
                }
                StringBuffer methodCode = new StringBuffer();
                methodCode.append(ClassUtils.generateMethodDeclaration(method) + "{");

                if (!method.getReturnType().equals(Void.TYPE)) {
                    methodCode.append("return ");
                }
                methodCode.append(generateProxyInvokeCode(prxoyFieldName, method));
                methodCode.append(" }");

                CtMethod ctMethod = CtMethod.make(methodCode.toString(), proxyCtClass);
                proxyCtClass.addMethod(ctMethod);
            }

            cacheCTClass(className, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            log.error(proxyObjClass.toString(), e);
        }

        return null;
    }

    /**
     * @param proxyObjClass 需要代理的类或该类的某一接口
     */
    public static <P> P enhanceClass0(Object proxyObj, Class<?> proxyObjClass, String packageName, String proxyCtClassName) {
        CtClass proxyCtClass = pool.getOrNull(proxyCtClassName);
        if (proxyCtClass == null) {
            proxyCtClass = generateEnhanceClassProxyClass(proxyObjClass, packageName);
        }

        if (proxyCtClass != null) {
            try {
                return (P) proxyCtClass.toClass().getConstructor(proxyObjClass).newInstance(proxyObj);
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
        String proxyCtClassName = packageName + "." + proxyObjClass.getSimpleName() + "$JavassistProxy";

        return enhanceClass0(proxyObj, proxyObjClass, packageName, proxyCtClassName);
    }

    public static <P> P enhanceClass(ProxyDefinition definition, Class<P> interfaceClass) {
        Object proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();

        if (!interfaceClass.isAssignableFrom(proxyObjClass)) {
            return null;
        }

        String packageName = definition.getPackageName();
        String proxyCtClassName = packageName + "." + interfaceClass.getSimpleName() + "$JavassistProxy";

        return enhanceClass0(proxyObj, interfaceClass, packageName, proxyCtClassName);
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
        return pool;
    }
}
