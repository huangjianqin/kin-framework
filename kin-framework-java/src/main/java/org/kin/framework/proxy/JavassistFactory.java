package org.kin.framework.proxy;

import javassist.*;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * 利用javassist字节码技术增强代理类 调用速度更快
 *
 * @author huangjianqin
 * @date 2020-01-11
 */
public final class JavassistFactory implements ProxyFactory {
    /** 单例 */
    public static final JavassistFactory INSTANCE = new JavassistFactory();

    /** 代理类中, 实现类默认字段名 */
    public static final String DEFAULT_INSTANCE_FIELD_NAME = "inst";
    /** 代理类中, 方法参数命名, $1, $2, $3==, $0=this */
    public static final String METHOD_DECLARATION_PARAM_NAME = "$";

    private static final ClassPool POOL = Javassists.getPool();

    private JavassistFactory() {
    }

    /**
     * 为ProxyInvoker的invoker方法生成目标方法调用代码
     *
     * @param serviceFieldName 实现类在代理类中的字段名
     * @param target           目标方法
     */
    private String generateProxyInvokerInvokeCode(String serviceFieldName, Method target) {
        StringBuilder invokeCode = new StringBuilder();

        Class<?> returnType = target.getReturnType();
        if (!returnType.equals(Void.TYPE)) {
            invokeCode.append("result = ");
        }

        StringBuilder oneLineCode = new StringBuilder();
        oneLineCode.append(serviceFieldName.concat(".").concat(target.getName()).concat("("));

        Class<?>[] paramTypes = target.getParameterTypes();
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
     * @param serviceClass     实现类
     * @param target           目标方法
     * @param proxyCtClassName 代理类名
     */
    private CtClass generateEnhanceMethodProxyClass(Class<?> serviceClass, Method target, String proxyCtClassName) {
        CtClass proxyCtClass = POOL.makeClass(proxyCtClassName);
        try {
            //实现接口
            proxyCtClass.addInterface(POOL.getCtClass(ProxyInvoker.class.getName()));

            //添加成员域
            String serviceFieldName = DEFAULT_INSTANCE_FIELD_NAME;
            CtField serviceCtField = new CtField(POOL.get(serviceClass.getName()), serviceFieldName, proxyCtClass);
            serviceCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(serviceCtField);

            String methodFieldName = "method";
            CtField methodCtField = new CtField(POOL.get(Method.class.getName()), methodFieldName, proxyCtClass);
            methodCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(methodCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{POOL.get(serviceClass.getName()), POOL.get(Method.class.getName())}, proxyCtClass);
            ctConstructor.setBody("{$0.".concat(serviceFieldName).concat(" = $1;").concat("$0.").concat(methodFieldName).concat(" = $2;}"));
            proxyCtClass.addConstructor(ctConstructor);

            //方法体
            //invoke
            CtMethod invokeCtMethod = new CtMethod(POOL.get(Object.class.getName()), "invoke",
                    new CtClass[]{POOL.get(Object[].class.getName())}, proxyCtClass);
            invokeCtMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
            invokeCtMethod.setExceptionTypes(new CtClass[]{POOL.get(Exception.class.getName())});
            StringBuilder methodBody = new StringBuilder();
            methodBody.append("{");
            methodBody.append("Object result = null;");
            methodBody.append(generateProxyInvokerInvokeCode(serviceFieldName, target));
            methodBody.append("return result; }");
            invokeCtMethod.setBody(methodBody.toString());

            proxyCtClass.addMethod(invokeCtMethod);

            //getProxyObj
            CtMethod getProxyObjCtMethod = new CtMethod(POOL.get(serviceClass.getName()), "getProxyObj", null, proxyCtClass);
            getProxyObjCtMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
            methodBody = new StringBuilder();
            methodBody.append("{");
            methodBody.append("return ".concat(serviceFieldName).concat("; }"));
            getProxyObjCtMethod.setBody(methodBody.toString());

            proxyCtClass.addMethod(getProxyObjCtMethod);

            //getMethod
            CtMethod getMethodCtMethod = new CtMethod(POOL.get(Method.class.getName()), "getMethod", null, proxyCtClass);
            getMethodCtMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
            methodBody = new StringBuilder();
            methodBody.append("{");
            methodBody.append("return ".concat(methodFieldName).concat("; }"));
            getMethodCtMethod.setBody(methodBody.toString());

            proxyCtClass.addMethod(getMethodCtMethod);

            Javassists.cacheCTClass(proxyCtClassName, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }

    /**
     * 生成代理某方法调用的代理类
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> ProxyInvoker<T> enhanceMethod(MethodDefinition<T> definition) {
        Object service = definition.getService();
        Class<?> serviceClass = service.getClass();
        Method target = definition.getMethod();
        String packageName;
        if (Objects.nonNull(serviceClass.getPackage())) {
            //jdk 代理的类, 则无法获取getPackage()
            packageName = serviceClass.getPackage().getName();
        } else {
            packageName = target.getDeclaringClass().getPackage().getName();
        }
        String proxyCtClassName = packageName.concat(".").concat(serviceClass.getSimpleName().concat("$").concat(ClassUtils.getUniqueName(target))).concat("$JavassistProxy");

        Class<?> realProxyClass = null;
        try {
            realProxyClass = Class.forName(proxyCtClassName);
        } catch (ClassNotFoundException e) {
            //ignore
        }

        if (Objects.isNull(realProxyClass)) {
            CtClass proxyCtClass = POOL.getOrNull(proxyCtClassName);
            if (proxyCtClass == null) {
                proxyCtClass = generateEnhanceMethodProxyClass(serviceClass, target, proxyCtClassName);
            }

            try {
                realProxyClass = proxyCtClass.toClass();
            } catch (CannotCompileException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        try {
            return (ProxyInvoker<T>) realProxyClass.getConstructor(serviceClass, Method.class).newInstance(service, target);
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }
    //---------------------------------------------------------------------------------------------------------------------------------

    /**
     * 为目标方法生成方法调用代码
     */
    public String generateProxyInvokeCode(String serviceFieldName, Method target) {
        StringBuilder methodBody = new StringBuilder();

        if (!target.getReturnType().equals(Void.TYPE)) {
            methodBody.append("return ");
        }

        StringBuilder oneLineCode = new StringBuilder();
        oneLineCode.append(serviceFieldName.concat(".").concat(target.getName()).concat("("));

        Class<?>[] paramTypes = target.getParameterTypes();
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

        Class<?> returnType = target.getReturnType();
        if (returnType.isPrimitive()) {
            methodBody.append(oneLineCode.toString());
        } else {
            methodBody.append(ClassUtils.primitivePackage(target.getReturnType(), oneLineCode.toString()));
        }
        methodBody.append(";");

        return methodBody.toString();
    }

    /**
     * @param interfaceClass 需要继承类(实现接口)
     */
    private CtClass generateEnhanceClassProxyClass(Class<?> interfaceClass, String proxyCtClassName) {
        CtClass proxyCtClass = POOL.makeClass(proxyCtClassName);
        try {
            String interfaceName = interfaceClass.getName();
            if (interfaceClass.isInterface()) {
                //实现接口
                proxyCtClass.addInterface(POOL.getCtClass(interfaceName));
            } else {
                //只适用于父类有无参构造器
                //继承类
                proxyCtClass.setSuperclass(POOL.getCtClass(interfaceName));
            }

            //添加成员域
            String serviceFieldName = DEFAULT_INSTANCE_FIELD_NAME;
            CtField serviceCtField = new CtField(POOL.get(interfaceName), serviceFieldName, proxyCtClass);
            serviceCtField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyCtClass.addField(serviceCtField);

            //处理构造方法
            CtConstructor ctConstructor = new CtConstructor(new CtClass[]{POOL.get(interfaceName)}, proxyCtClass);
            ctConstructor.setBody("{$0.".concat(serviceFieldName).concat(" = $1;}"));
            proxyCtClass.addConstructor(ctConstructor);

            //类实现
            //invoke
            for (Method method : interfaceClass.getMethods()) {
                if (Modifier.isFinal(method.getModifiers())) {
                    //跳过final 方法
                    continue;
                }

                //参数CtClass
                CtClass[] parameterCtClass = Javassists.getParamCtClasses(POOL, method);

                CtMethod ctMethod = new CtMethod(POOL.get(method.getReturnType().getName()), method.getName(), parameterCtClass, proxyCtClass);
                ctMethod.setModifiers(Modifier.PUBLIC + Modifier.FINAL);
                ctMethod.setBody("{" +
                        generateProxyInvokeCode(serviceFieldName, method) +
                        " }");
                proxyCtClass.addMethod(ctMethod);
            }

            Javassists.cacheCTClass(proxyCtClassName, proxyCtClass);
            return proxyCtClass;
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }

    /**
     * @param service          实现类
     * @param serviceClass     可以是一个类, 也可以是接口, 如果是接口的话, 仅仅代理其接口方法, 如果是类的话, 仅仅代理public方法
     * @param packageName      包名
     * @param proxyCtClassName 代理类类名
     */
    @SuppressWarnings("unchecked")
    private <P> P enhanceClass0(
            Object service,
            Class<?> serviceClass,
            String packageName,
            String proxyCtClassName) {
        Class<?> realProxyClass = null;
        try {
            realProxyClass = Class.forName(proxyCtClassName);
        } catch (ClassNotFoundException e) {
            //ignore
        }

        if (Objects.isNull(realProxyClass)) {
            CtClass proxyCtClass = POOL.getOrNull(proxyCtClassName);
            if (proxyCtClass == null) {
                proxyCtClass = generateEnhanceClassProxyClass(serviceClass, packageName);
            }
            try {
                realProxyClass = proxyCtClass.toClass();
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }


        if (Objects.nonNull(realProxyClass)) {
            try {
                return (P) realProxyClass.getConstructor(serviceClass).newInstance(service);
            } catch (Exception e) {
                ExceptionUtils.throwExt(e);
            }
        }

        throw new IllegalStateException("proxy enhance encounter unknown error");
    }

    /**
     * 生成代理某个类(接口)的代理类
     */
    @Override
    public <P> P enhanceClass(ClassDefinition<P> definition) {
        P service = definition.getService();
        Class<?> serviceClass = service.getClass();
        Class<?> interfaceClass = definition.getInterfaceClass();
        String packageName = interfaceClass.getPackage().getName();
        String proxyCtClassName = packageName.concat(".").concat(interfaceClass.getSimpleName()).concat("$JavassistProxy");

        if (!interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalArgumentException(serviceClass.getCanonicalName() + " is not implement " + interfaceClass.getName());
        }

        return enhanceClass0(service, interfaceClass, packageName, proxyCtClassName);
    }
}
