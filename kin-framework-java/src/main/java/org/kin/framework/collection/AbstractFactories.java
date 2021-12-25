package org.kin.framework.collection;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.CollectionUtils;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 工厂管理抽象
 *
 * @author huangjianqin
 * @date 2021/12/25
 */
public abstract class AbstractFactories<F extends Factory> {
    /** key -> 工厂创建的实例类型, value -> 工厂实例 */
    private volatile Map<Class<?>, F> factories = Collections.emptyMap();
    /** 工厂接口 */
    private final Class<F> factoryType;

    @SuppressWarnings("unchecked")
    protected AbstractFactories() {
        //获取父类泛型类型
        List<Class<?>> types = ClassUtils.getSuperClassGenericRawTypes(getClass());
        factoryType = (Class<F>) types.get(0);
    }

    /**
     * 根据类型获取工厂
     */
    public final F getFactory(Class<?> type) {
        if (Objects.isNull(type)) {
            throw new IllegalArgumentException("type is null!");
        }
        // 这里之所以要遍历该类所有父类和实现接口, 因为在处理动态类型(Object)时, 有可能实例是一些不在访问范围内(private)的Collection或Map实现,
        // 比如java.util.Arrays$ArrayList, 可以正常write, 但我们无法反序列成java.util.Arrays$ArrayList,
        // 不过, 我们可以序列化成java.util.ArrayList, 只要内容一致即可
        // 获取所有继承父类(包含自己)
        List<Class<?>> classes = ClassUtils.getAllClasses(type);
        for (Class<?> claxx : classes) {
            F ret = factories.get(claxx);
            if (Objects.nonNull(ret)) {
                return ret;
            }

            // 获取所有实现接口
            for (Class<?> interfaceClass : claxx.getInterfaces()) {
                ret = factories.get(interfaceClass);
                if (Objects.nonNull(ret)) {
                    return ret;
                }
            }
        }
        throw new IllegalArgumentException(String.format("can't not find factory for type '%s'", type.getCanonicalName()));
    }

    /**
     * 暴露给user, 注册工厂
     */
    public synchronized final void register(F factory) {
        List<Type> actualTypes = ClassUtils.getSuperInterfacesGenericActualTypes(factoryType, factory.getClass());
        register((Class<?>) actualTypes.get(0), factory);
    }

    /**
     * 暴露给user, 注册工厂
     */
    public synchronized void register(Class<?> type, F factory) {
        if (Objects.isNull(type) || Objects.isNull(factory)) {
            throw new IllegalArgumentException("type or factory is null!");
        }
        register(Collections.singletonMap(type, factory));
    }

    /**
     * 暴露给user, 注册工厂
     */
    public synchronized final void register(Map<Class<?>, F> newFactories) {
        if (CollectionUtils.isEmpty(newFactories)) {
            return;
        }
        Map<Class<?>, F> factories = new HashMap<>(this.factories);
        factories.putAll(newFactories);
        this.factories = factories;
    }
}
