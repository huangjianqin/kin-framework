package org.kin.framework.proxy.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javassist.*;
import org.kin.framework.proxy.ProxyDefinition;
import org.kin.framework.proxy.ProxyInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
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
    private static final String GETPROXYOBj_METHOD_SIGNATURE = "public Object getProxyObj()";
    private static final String GETMETHOD_METHOD_SIGNATURE = "public " + Method.class.getName() + " getMethod()";

    private ProxyEnhanceUtils() {
    }

    private static String generateInvokeCode(String fieldName, Method method) {
        StringBuffer invokeCode = new StringBuffer();

        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(Void.TYPE)) {
            invokeCode.append("result = ");
        }

        StringBuffer oneLineCode = new StringBuffer();
        oneLineCode.append(fieldName + "." + method.getName() + "(");

        Class[] paramTypes = method.getParameterTypes();
        StringJoiner paramBody = new StringJoiner(", ");
        for (int i = 0; i < paramTypes.length; i++) {
            paramBody.add(org.kin.framework.utils.ClassUtils.primitiveUnpackage(paramTypes[i], "params[" + i + "]"));
        }

        oneLineCode.append(paramBody.toString());
        oneLineCode.append(")");

        invokeCode.append(org.kin.framework.utils.ClassUtils.primitivePackage(returnType, oneLineCode.toString()));
        invokeCode.append(";");

        return invokeCode.toString();
    }

    /**
     * 利用javassist字节码技术生成方法代理类, 调用效率比反射要高
     */
    public static <S> ProxyInvoker<S> generateMethodProxy(ProxyDefinition<S> definition) {
        Object proxyObj = definition.getProxyObj();
        Class<?> proxyObjClass = proxyObj.getClass();
        Method method = definition.getMethod();
        String className = definition.getClassName();

        CtClass proxyClass = pool.makeClass(className);
        try {
            //实现接口
            proxyClass.addInterface(pool.getCtClass(ProxyInvoker.class.getName()));

            //添加成员域
            String prxoyFieldName = "proxy";
            CtField proxyField = new CtField(pool.get(proxyObjClass.getName()), prxoyFieldName, proxyClass);
            proxyField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyClass.addField(proxyField);

            String methodFieldName = "method";
            CtField methodField = new CtField(pool.get(Method.class.getName()), methodFieldName, proxyClass);
            methodField.setModifiers(Modifier.PRIVATE | Modifier.FINAL);
            proxyClass.addField(methodField);

            //处理构造方法
            CtConstructor constructor = new CtConstructor(new CtClass[]{pool.get(proxyObjClass.getName()), pool.get(Method.class.getName())}, proxyClass);
            constructor.setBody("{$0." + prxoyFieldName + " = $1;" + "$0." + methodFieldName + " = $2;}");
            proxyClass.addConstructor(constructor);

            //方法体
            //invoke
            StringBuffer methodBody = new StringBuffer();
            methodBody.append(INVOKE_METHOD_SIGNATURE + "{");
            methodBody.append("Object result = null;");
            methodBody.append(generateInvokeCode(prxoyFieldName, method));
            methodBody.append("return result; }");

            CtMethod invokeMethod = CtMethod.make(methodBody.toString(), proxyClass);
            proxyClass.addMethod(invokeMethod);

            //getProxyObj
            methodBody = new StringBuffer();
            methodBody.append(GETPROXYOBj_METHOD_SIGNATURE + "{");
            methodBody.append("return " + prxoyFieldName + "; }");

            CtMethod getProxyObjMethod = CtMethod.make(methodBody.toString(), proxyClass);
            proxyClass.addMethod(getProxyObjMethod);

            //getMethod
            methodBody = new StringBuffer();
            methodBody.append(GETMETHOD_METHOD_SIGNATURE + "{");
            methodBody.append("return " + methodFieldName + "; }");

            CtMethod getMethodMethod = CtMethod.make(methodBody.toString(), proxyClass);
            proxyClass.addMethod(getMethodMethod);

            Class<?> realProxyClass = proxyClass.toClass();
            ProxyInvoker proxyMethodInvoker = (ProxyInvoker) realProxyClass.getConstructor(proxyObjClass, Method.class).newInstance(proxyObj, method);

            cacheCTClass(className, proxyClass);

            return proxyMethodInvoker;
        } catch (Exception e) {
            log.error(method.toString(), e);
        }

        return null;
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
        return pool;
    }
}
