package org.kin.framework.beans;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SysUtils;
import org.kin.framework.utils.UnsafeUtil;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2021/9/8
 */
public final class BeanUtils {
    /** 使用支持字节码增强 */
    public static final boolean ENHANCE;

    static {
        Class<?> byteBuddyClass = null;
        try {
            byteBuddyClass = Class.forName("net.bytebuddy.ByteBuddy");
        } catch (Exception e) {
            //ignore
        }

        ENHANCE = Objects.nonNull(byteBuddyClass);
    }

    /** 是否开启深复制, 从app jvm参数获取开关, -D开头 */
    public static final boolean DEEP = SysUtils.getBoolSysProperty("kin.beans.copy.deep", false);
    /** {@link Introspector}是否忽略所有与指定class的有关联的{@link java.beans.BeanInfo}, 包括其父类 */
    public static final boolean SHOULD_INTROSPECTOR_IGNORE_BEANINFO_CLASSES = SysUtils.getBoolSysProperty("kin.beans.should.introspector.ignoreBeanInfoClasses", false);

    /** 使用者自定义{@link BeanInfoFactory}实例 */
    private static final CopyOnWriteArrayList<BeanInfoFactory> BEAN_INFO_FACTORIES = new CopyOnWriteArrayList<>();
    /** soft reference && 5 min ttl */
    private static final Cache<Class<?>, BeanInfoDetails> BEAN_INFO_CACHE =
            CacheBuilder.newBuilder()
                    .softValues()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();

    private BeanUtils() {
    }

    //-------------------------------------------bean copy custom param
    public static void registerBeanInfoFactory(Collection<BeanInfoFactory> beanInfoFactories) {
        BEAN_INFO_FACTORIES.addAll(beanInfoFactories);
    }

    public static void registerBeanInfoFactory(BeanInfoFactory... beanInfoFactories) {
        BEAN_INFO_FACTORIES.addAll(Arrays.asList(beanInfoFactories));
    }

    public static void registerBeanInfoFactoryFromExtensions() {
        BEAN_INFO_FACTORIES.addAll(ExtensionLoader.getExtensions(BeanInfoFactory.class));
    }

    //---------------------------------------------------------------------------------------------------------

    /**
     * bean field字段复制
     */
    public static void copyProperties(Object source, Object target) {
        if (ENHANCE) {
            ByteBuddyBeanCopy.INSTANCE.copyProperties(source, target);
        } else {
            if (UnsafeUtil.hasUnsafe()) {
                UnsafeBeanCopy.INSTANCE.copyProperties(source, target);
            } else {
                ReflectionBeanCopy.INSTANCE.copyProperties(source, target);
            }
        }
    }

    /**
     * bean field字段复制
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        T target = ClassUtils.instance(targetClass);
        copyProperties(source, target);
        return target;
    }

    /**
     * 获取{@link BeanInfoDetails}
     */
    public static BeanInfoDetails getBeanInfo(Class<?> claxx) {
        try {
            return BEAN_INFO_CACHE.get(claxx, () -> new BeanInfoDetails(getBeanInfo0(claxx)));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(String.format("can't found bean info for class '%s', due to", claxx.getCanonicalName()), e);
        }
    }

    /**
     * 获取{@link BeanInfoDetails}
     */
    private static BeanInfo getBeanInfo0(Class<?> claxx) throws IntrospectionException {
        for (BeanInfoFactory beanInfoFactory : BeanUtils.BEAN_INFO_FACTORIES) {
            BeanInfo beanInfo = beanInfoFactory.getBeanInfo(claxx);
            if (beanInfo != null) {
                return beanInfo;
            }
        }

        return (SHOULD_INTROSPECTOR_IGNORE_BEANINFO_CLASSES ?
                Introspector.getBeanInfo(claxx, Introspector.IGNORE_ALL_BEANINFO) :
                Introspector.getBeanInfo(claxx));
    }
}
