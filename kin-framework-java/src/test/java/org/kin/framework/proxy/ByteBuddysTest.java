package org.kin.framework.proxy;

import org.kin.framework.utils.ClassUtils;

import java.lang.reflect.Method;

/**
 * @author huangjianqin
 * @date 2020/12/23
 */
public class ByteBuddysTest {
    public static void main(String[] args) {
        Class<?> claxx = ProxyInvoker.class;
        for (Method method : claxx.getMethods()) {
            System.out.println(method.toGenericString());
            System.out.println(ClassUtils.generateMethodDeclaration(method));
            System.out.println("--------------");
        }

        ProxyFactory proxyFactory = Proxys.byteBuddy();

        AddService addService = new AddService();

        Method targetMethod = AddService.class.getMethods()[0];
        System.out.println(targetMethod);
        ProxyInvoker<AddService> proxyInvoker = proxyFactory.enhanceMethod(new MethodDefinition<>(addService, targetMethod));
        try {
            System.out.println(proxyInvoker.invoke(1, 1));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("--------------");
        AddService addServiceProxy = proxyFactory.enhanceClass(new ClassDefinition<>(addService));
        System.out.println(addServiceProxy.add(1, 1));
        System.out.println(addService);
        System.out.println(addServiceProxy.equals(addService));
        System.out.println(addServiceProxy.get(addService));
        System.out.println(addService.addItem(1));
    }
}
