package org.kin.framework.proxy;

/**
 * 代理类定义
 *
 * @author huangjianqin
 * @date 2020-01-16
 */
public class ClassDefinition<T> {
    /** 实现类 */
    private final T service;
    /** 目标类(接口) */
    private final Class<T> interfaceClass;

    @SuppressWarnings("unchecked")
    public ClassDefinition(T service) {
        this(service, (Class<T>) service.getClass());
    }

    public ClassDefinition(T service, Class<T> interfaceClass) {
        this.service = service;
        this.interfaceClass = interfaceClass;
    }

    //getter
    public T getService() {
        return service;
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }
}
