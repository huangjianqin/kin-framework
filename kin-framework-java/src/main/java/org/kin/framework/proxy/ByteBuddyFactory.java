package org.kin.framework.proxy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.SysUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 利用byte buddy字节码技术增强代理类 调用速度更快
 *
 * @author huangjianqin
 * @date 2020/12/22
 */
public final class ByteBuddyFactory implements ProxyFactory {
    /** 单例 */
    public static final ByteBuddyFactory INSTANCE = new ByteBuddyFactory();
    /** 代理类中, 实现类默认字段名 */
    public static final String DEFAULT_SERVICE_FIELD_NAME = "service";
    /** 代理类中, 实现代理方法(Method实例)默认字段名 */
    public static final String DEFAULT_METHOD_FIELD_NAME = "method";

    private ByteBuddyFactory() {
    }

    /**
     * 增强代理某个方法的代理类
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> ProxyInvoker<T> enhanceMethod(MethodDefinition<T> definition) {
        T service = definition.getService();
        Class<?> serviceClass = service.getClass();
        Method target = definition.getMethod();
        String packageName = serviceClass.getPackage().getName();
        String className = packageName.concat(".").concat(serviceClass.getSimpleName().concat("$").concat(target.getName())).concat("$ByteBuddyProxy");
        ;
        try {
            //设置泛型ProxyInvoker接口
            List<TypeDefinition> genericArgDefinitions = Collections.singletonList(new TypeDescription.ForLoadedType(serviceClass));
            TypeDescription.Generic generic = Optional.ofNullable(ProxyInvoker.class.getDeclaringClass())
                    .map(TypeDefinition.Sort::describe)
                    .orElse(null);
            TypeDefinition proxyType = TypeDescription.Generic.Builder.parameterizedType(
                    new TypeDescription.ForLoadedType(ProxyInvoker.class)
                    , generic, genericArgDefinitions).build();

            return (ProxyInvoker<T>) new ByteBuddy(ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V8))
                    .subclass(proxyType)
                    .name(className)
                    //定义field
                    .defineField(DEFAULT_SERVICE_FIELD_NAME, serviceClass, Modifier.PRIVATE + Modifier.FINAL)
                    .defineField(DEFAULT_METHOD_FIELD_NAME, Method.class, Modifier.PRIVATE + Modifier.FINAL)
                    //定义constructor
                    .defineConstructor(Modifier.PUBLIC)
                    .withParameters(serviceClass, Method.class)
                    .intercept(MethodCall.invoke(Object.class.getDeclaredConstructor())
                            .andThen(FieldAccessor.ofField(DEFAULT_SERVICE_FIELD_NAME).setsArgumentAt(0))
                            .andThen(FieldAccessor.ofField(DEFAULT_METHOD_FIELD_NAME).setsArgumentAt(1)))
                    //定义方法, 与ProxyInvoker接口方法一致
                    .method(ElementMatchers.named("getProxyObj"))
                    .intercept(FieldAccessor.ofField(DEFAULT_SERVICE_FIELD_NAME))
                    .method(ElementMatchers.named("getMethod"))
                    .intercept(FieldAccessor.ofField(DEFAULT_METHOD_FIELD_NAME))
                    .method(ElementMatchers.named("invoke"))
                    .intercept(MethodCall.invoke(target)
                            .onField(DEFAULT_SERVICE_FIELD_NAME)
                            .withArgumentArrayElements(0)
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .make()
                    .load(SysUtils.getClassLoader(ProxyInvoker.class))
                    .getLoaded()
                    .getConstructor(serviceClass, target.getClass())
                    .newInstance(service, target);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            ExceptionUtils.throwExt(e);
        }

        //never reach
        throw new IllegalStateException("encounter unknown error");
    }


    /**
     * 生成代理某个类(接口)的代理类, 仅仅实现该interfaceClass的public(接口)方法
     */
    @Override
    @SuppressWarnings("unchecked")
    public <P> P enhanceClass(ClassDefinition<P> definition) {
        P service = definition.getService();
        Class<?> serviceClass = service.getClass();
        Class<P> interfaceClass = definition.getInterfaceClass();
        String packageName = interfaceClass.getPackage().getName();

        if (!interfaceClass.isAssignableFrom(serviceClass)) {
            throw new IllegalArgumentException(serviceClass.getCanonicalName() + " is not implement " + interfaceClass.getName());
        }

        String proxyClassName = packageName.concat(".").concat(interfaceClass.getSimpleName()).concat("$ByteBuddyProxy");
        try {
            ByteBuddy byteBuddy = new ByteBuddy(ClassFileVersion.ofThisVm(ClassFileVersion.JAVA_V8));
            Class<?> supperClass;
            DynamicType.Builder<P> builder;
            if (interfaceClass.isInterface()) {
                supperClass = Object.class;
                builder = (DynamicType.Builder<P>) byteBuddy.subclass(supperClass).implement(interfaceClass);
            } else {
                supperClass = interfaceClass;
                builder = byteBuddy.subclass(interfaceClass);
            }

            builder = builder.name(proxyClassName)
                    //定义field
                    .defineField(DEFAULT_SERVICE_FIELD_NAME, serviceClass, Modifier.PRIVATE + Modifier.FINAL)
                    //定义constructor
                    .defineConstructor(Modifier.PUBLIC)
                    .withParameters(serviceClass)
                    //只适用于父类有无参构造器
                    .intercept(MethodCall.invoke(supperClass.getDeclaredConstructor())
                            .andThen(FieldAccessor.ofField(DEFAULT_SERVICE_FIELD_NAME).setsArgumentAt(0)));

            for (Method method : interfaceClass.getMethods()) {
                if (Modifier.isFinal(method.getModifiers())) {
                    //跳过final 方法
                    continue;
                }

                if (method.getDeclaringClass().equals(Object.class)) {
                    //跳过Object方法
                    continue;
                }

                if (method.isDefault()) {
                    //跳过default
                    continue;
                }

                builder.method(ElementMatchers.is(method))
                        .intercept(MethodCall.invoke(method)
                                .onField(DEFAULT_SERVICE_FIELD_NAME)
                                .withArgumentArrayElements(0)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
            }

            return (P) builder
                    .make()
                    .load(SysUtils.getClassLoader(interfaceClass))
                    .getLoaded()
                    .getConstructor(interfaceClass)
                    .newInstance(service);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            ExceptionUtils.throwExt(e);
        }

        //never reach
        throw new IllegalStateException("encounter unknown error");
    }
}
