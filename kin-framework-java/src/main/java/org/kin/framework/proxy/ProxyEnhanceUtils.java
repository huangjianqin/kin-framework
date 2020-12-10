package org.kin.framework.proxy;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;

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
    private static final ClassPool POOL = ClassPool.getDefault();
    private static final Multimap<String, CtClass> CTCLASS_CACHE = HashMultimap.create();

    /** 代理类中, 实现类默认字段名 */
    public static final String DEFAULT_PROXY_FIELD_NAME = "proxy";
    /** 代理类中, 方法参数, $1, $2, $3==, $0=this */
    public static final String METHOD_DECLARATION_PARAM_NAME = "$";

    private ProxyEnhanceUtils() {
    }

    /**
     * 为了ProxyInvoker的invoker方法生成代理方法调用代码
     *
     * @param fieldName   实现类在代理类中的字段名
     * @param proxyMethod 代理方法
     */
    private static String generateProxyInvokerInvokeCode(String fieldName, Method proxyMethod) {
        StringBuilder invokeCode = new StringBuilder();

        Class<?> returnType = proxyMethod.getReturnType();
        if (!returnType.equals(Void.TYPE)) {
            invokeCode.append("result = ");
        }

        StringBuilder oneLineCode = new StringBuilder();
        oneLineCode.append(fieldName.concat(".").concat(proxyMethod.getName()).concat("("));

        Class[] paramTypes = proxyMethod.getParameterTypes();
        StringJoiner paramBody = new StringJoiner(", ");
        for (int i = 0; i < paramTypes.length; i++) {
            //因为ProxyInvoker的invoker方法只有一个参数, Object[], 所以从param0取方法
            paramBody.add(org.kin.framework.utils.ClassUtils.primitiveUnpackage(paramTypes[i], METHOD_DECLARATION_PARAM_NAME + "1[" + i + "]"));
        }

        oneLineCode.append(paramBody.toString());
        oneLineCode.append(")");

        invokeCode.append(org.kin.framework.utils.ClassUtils.primitivePackage(returnType, oneLineCode.toString()));
        invokeCode.append(";");

        return invokeCode.toString();
    }

    /**
     * 生成代理某方法调用的代理类
     *
     * @param proxyObjClass    实现类
     * @param proxyMethod      代理方法
     * @param proxyCtClassName 代理类名
     */
    private static CtClass generateEnhanceMethodProxyClass(Class<?> proxyObjClass, Method proxyMethod, String proxyCtClassName) {
        CtClass proxyCtClass = POOL.makeClass(proxyCtClassName);
        try {
            //实现接口
            proxyCtClass.addInterface(POOL.getCtClass(ProxyInvoker.class.getCanonicalName()));

            //添加成员域
            String prxoyFieldName = DEFAULT_PROXY_FIELD_NAME;
            CtField proxyCtField = new CtField(POOL.get(proxyObjClass.getCanonicalName()), prxoyFieldName, proxyCtClass);
            proxyCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyCtField);

            String methodFieldName = "method";
            CtField methodCtField = new CtField(POOL.get(Method.class.getCanonicalName()), methodFieldName, proxyCtClass);
            methodCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(methodCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{POOL.get(proxyObjClass.getCanonicalName()), POOL.get(Method.class.getCanonicalName())}, proxyCtClass);
            ctConstructor.setBody("{$0.".concat(prxoyFieldName).concat(" = $1;").concat("$0.").concat(methodFieldName).concat(" = $2;}"));
            proxyCtClass.addConstructor(ctConstructor);

            //方法体
            //invoke
            CtMethod invokeCtMethod = new CtMethod(POOL.get(Object.class.getCanonicalName()), "invoke",
                    new CtClass[]{POOL.get(Object[].class.getCanonicalName())}, proxyCtClass);
            invokeCtMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
            invokeCtMethod.setExceptionTypes(new CtClass[]{POOL.get(Exception.class.getCanonicalName())});
            StringBuilder methodBody = new StringBuilder();
            methodBody.append("{");
            methodBody.append("Object result = null;");
            methodBody.append(generateProxyInvokerInvokeCode(prxoyFieldName, proxyMethod));
            methodBody.append("return result; }");
            invokeCtMethod.setBody(methodBody.toString());

            proxyCtClass.addMethod(invokeCtMethod);

            //getProxyObj
            CtMethod getProxyObjCtMethod = new CtMethod(POOL.get(proxyObjClass.getCanonicalName()), "getProxyObj", null, proxyCtClass);
            getProxyObjCtMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
            methodBody = new StringBuilder();
            methodBody.append("{");
            methodBody.append("return ".concat(prxoyFieldName).concat("; }"));
            getProxyObjCtMethod.setBody(methodBody.toString());

            proxyCtClass.addMethod(getProxyObjCtMethod);

            //getMethod
            CtMethod getMethodCtMethod = new CtMethod(POOL.get(Method.class.getCanonicalName()), "getMethod", null, proxyCtClass);
            getMethodCtMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
            methodBody = new StringBuilder();
            methodBody.append("{");
            methodBody.append("return ".concat(methodFieldName).concat("; }"));
            getMethodCtMethod.setBody(methodBody.toString());

            proxyCtClass.addMethod(getMethodCtMethod);

            cacheCTClass(proxyCtClassName, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }

    /**
     * 增强某个方法代理的调用
     */
    @SuppressWarnings("unchecked")
    public static <T> ProxyInvoker<T> enhanceMethod(ProxyMethodDefinition<T> definition) {
        Object proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();
        Method proxyMethod = definition.getMethod();
        String proxyCtClassName = definition.getClassName();

        Class<?> realProxyClass = null;
        try {
            realProxyClass = Class.forName(proxyCtClassName);
        } catch (ClassNotFoundException e) {
            //ignore
        }

        if (Objects.isNull(realProxyClass)) {
            CtClass proxyCtClass = POOL.getOrNull(proxyCtClassName);
            if (proxyCtClass == null) {
                proxyCtClass = generateEnhanceMethodProxyClass(proxyObjClass, proxyMethod, proxyCtClassName);
            }

            try {
                realProxyClass = proxyCtClass.toClass();
            } catch (CannotCompileException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        try {
            return (ProxyInvoker<T>) realProxyClass.getConstructor(proxyObjClass, Method.class).newInstance(proxyObj, proxyMethod);
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }
    //---------------------------------------------------------------------------------------------------------------------------------

    /**
     * 为目标方法生成代理方法调用代码
     */
    public static String generateProxyInvokeCode(String proxyFieldName, Method proxyMethod) {
        StringBuilder methodBody = new StringBuilder();

        if (!proxyMethod.getReturnType().equals(Void.TYPE)) {
            methodBody.append("return ");
        }

        StringBuilder oneLineCode = new StringBuilder();
        oneLineCode.append(proxyFieldName.concat(".").concat(proxyMethod.getName()).concat("("));

        Class<?>[] paramTypes = proxyMethod.getParameterTypes();
        StringJoiner paramBody = new StringJoiner(", ");
        for (int i = 0; i < paramTypes.length; i++) {
            int paramNum = i + 1;
            if (paramTypes[i].isPrimitive()) {
                paramBody.add(METHOD_DECLARATION_PARAM_NAME + paramNum);
            } else {
                paramBody.add(ClassUtils.primitivePackage(paramTypes[i], METHOD_DECLARATION_PARAM_NAME + paramNum));
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
            String proxyClassCanonicalName = proxyClass.getCanonicalName();
            if (proxyClass.isInterface()) {
                //实现接口
                proxyCtClass.addInterface(POOL.getCtClass(proxyClassCanonicalName));
            } else {
                //继承类
                proxyCtClass.setSuperclass(POOL.getCtClass(proxyClassCanonicalName));
            }

            //添加成员域
            String prxoyFieldName = DEFAULT_PROXY_FIELD_NAME;
            CtField proxyCtField = new CtField(POOL.get(proxyClassCanonicalName), prxoyFieldName, proxyCtClass);
            proxyCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(proxyCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{POOL.get(proxyClassCanonicalName)}, proxyCtClass);
            ctConstructor.setBody("{$0.".concat(prxoyFieldName).concat(" = $1;}"));
            proxyCtClass.addConstructor(ctConstructor);

            //类实现
            //invoke
            for (Method method : proxyClass.getMethods()) {
                if (Modifier.isFinal(method.getModifiers())) {
                    //跳过final 方法
                    continue;
                }

                //参数CtClass
                CtClass[] parameterCtClass = getParamCtClasses(method);

                CtMethod ctMethod = new CtMethod(POOL.get(method.getReturnType().getCanonicalName()), method.getName(), parameterCtClass, proxyCtClass);
                ctMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
                ctMethod.setBody("{" +
                        methodBodyConstructor.construct(prxoyFieldName, method) +
                        " }");
                proxyCtClass.addMethod(ctMethod);
            }

            cacheCTClass(className, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }

    /**
     * @param proxyObjClass 需要代理的类或该类的某一接口
     * @param proxyObj  实现类
     * @param proxyObjClass 代理类, 可以是一个类, 也可以是接口, 如果是接口的话, 仅仅代理其接口方法, 如果是类的话, 仅仅代理public方法
     * @param packageName 包名
     * @param proxyCtClassName 代理类类名
     * @param methodBodyConstructor 代理方法代码自定义
     */
    @SuppressWarnings("unchecked")
    private static <P> P enhanceClass0(
            Object proxyObj,
            Class<?> proxyObjClass,
            String packageName,
            String proxyCtClassName,
            MethodBodyConstructor methodBodyConstructor) {
        Class<?> realProxyClass = null;
        try {
            realProxyClass = Class.forName(proxyCtClassName);
        } catch (ClassNotFoundException e) {
            //ignore
        }

        if (Objects.isNull(realProxyClass)) {
            CtClass proxyCtClass = POOL.getOrNull(proxyCtClassName);
            if (proxyCtClass == null) {
                proxyCtClass = generateEnhanceClassProxyClass(proxyObjClass, packageName, methodBodyConstructor);
            }
            try {
                realProxyClass = proxyCtClass.toClass();
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }


        if (Objects.nonNull(realProxyClass)) {
            try {
                return (P) realProxyClass.getConstructor(proxyObjClass).newInstance(proxyObj);
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }

    /**
     * 生成代理class的代理类
     */
    public static <P> P enhanceClass(ProxyDefinition<P> definition) {
        P proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();
        String packageName = definition.getPackageName();
        String proxyCtClassName = packageName.concat(".").concat(proxyObjClass.getSimpleName()).concat("$JavassistProxy");

        return enhanceClass0(proxyObj, proxyObjClass, packageName, proxyCtClassName, definition.getMethodBodyConstructor());
    }

    /**
     * 生成代理interfaceClass的代理类, 仅仅实现该interfaceClass的接口方法
     */
    public static <P> P enhanceClass(ProxyDefinition<P> definition, Class<P> interfaceClass) {
        P proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();

        if (!interfaceClass.isAssignableFrom(proxyObjClass)) {
            throw new IllegalArgumentException(proxyObjClass.getCanonicalName() + " is not implement " + interfaceClass.getCanonicalName());
        }

        String packageName = definition.getPackageName();
        String proxyCtClassName = packageName.concat(".").concat(interfaceClass.getSimpleName()).concat("$JavassistProxy");

        return enhanceClass0(proxyObj, interfaceClass, packageName, proxyCtClassName, definition.getMethodBodyConstructor());
    }

    //---------------------------------------------------------------------------------------------------------------------------------

    /**
     * 将方法参数类型转换成CtClass[]
     */
    public static CtClass[] getParamCtClasses(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        CtClass[] parameterCtClass = new CtClass[parameterTypes.length];

        for (int i = 0; i < parameterCtClass.length; i++) {
            try {
                parameterCtClass[i] = POOL.get(parameterTypes[i].getCanonicalName());
            } catch (NotFoundException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        return parameterCtClass;
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

    public static void cacheCTClass(String className, CtClass ctClass) {
        CTCLASS_CACHE.put(className, ctClass);
    }


    public static ClassPool getPool() {
        return POOL;
    }

    //---------------------------------------------------------------------------------------------------------------------------------
}
