package org.kin.framework.beans;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.KinServiceLoader;
import org.kin.framework.utils.SysUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author huangjianqin
 * @date 2021/9/8
 */
public final class BeanUtils {
    /** 使用支持字节码增强 */
    private static final boolean ENHANCE;

    static {
        Class<?> byteBuddyClass = null;
        try {
            byteBuddyClass = Class.forName("net.bytebuddy.ByteBuddy");
        } catch (Exception e) {
            //ignore
        }

        if (Objects.nonNull(byteBuddyClass)) {
            ENHANCE = true;
        } else {
            ENHANCE = false;
        }
    }

    /** 是否开启深复制, 从app jvm参数获取开关, -D开头 */
    static final boolean DEEP = SysUtils.getBoolSysProperty("kin.beans.copy.deep", false);
    /** 使用者自定义{@link BeanInfoFactory}实例 */
    static final CopyOnWriteArrayList<BeanInfoFactory> BEAN_INFO_FACTORIES = new CopyOnWriteArrayList<>();

    private BeanUtils() {
    }

    //-------------------------------------------bean copy custom param
    public static void registerBeanInfoFactory(Collection<BeanInfoFactory> beanInfoFactories) {
        BEAN_INFO_FACTORIES.addAll(beanInfoFactories);
    }

    public static void registerBeanInfoFactory(BeanInfoFactory... beanInfoFactories) {
        BEAN_INFO_FACTORIES.addAll(Arrays.asList(beanInfoFactories));
    }

    public static void registerBeanInfoFactory(KinServiceLoader loader) {
        BEAN_INFO_FACTORIES.addAll(loader.getExtensions(BeanInfoFactory.class));
    }

    //---------------------------------------------------------------------------------------------------------

    /**
     * bean field字段复制
     */
    public static void copyProperties(Object source, Object target) {
        if (ENHANCE) {
            ByteBuddyBeanCopy.INSTANCE.copyProperties(source, target);
        } else {
            ReflectionBeanCopy.INSTANCE.copyProperties(source, target);
        }
    }

    /**
     * bean field字段复制
     */
    public static <T> T copyProperties(Object source, Class<T> targetClass) {
        T target = ClassUtils.instance(targetClass);
        copyProperties(source, target);
        return (T) target;
    }
}
