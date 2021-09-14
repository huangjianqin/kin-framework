package org.kin.framework.beans;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.SysUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * {@link BeanInfo}缓存
 *
 * @author huangjianqin
 * @date 2021/9/8
 */
final class BeanInfoCache {
    static final BeanInfoCache INSTANCE = new BeanInfoCache();
    /** soft reference && 30min ttl */
    private static final Cache<Class<?>, BeanInfoDetails> BEAN_INFO_CACHE =
            CacheBuilder.newBuilder()
                    .softValues()
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build();
    /** {@link Introspector}是否忽略所有与指定class的有关联的{@link BeanInfo}, 包括其父类 */
    private static final boolean SHOULD_INTROSPECTOR_IGNORE_BEANINFO_CLASSES = SysUtils.getBoolSysProperty("kin.beans.should.introspector.ignoreBeanInfoClasses", false);
    ;

    private BeanInfoCache() {
    }

    BeanInfoDetails get(Class<?> claxx) {
        try {
            return BEAN_INFO_CACHE.get(claxx, () -> new BeanInfoDetails(getBeanInfo(claxx)));
        } catch (ExecutionException e) {
            throw new IllegalArgumentException(String.format("can't found bean info for class '%s', due to", claxx.getCanonicalName()), e);
        }
    }

    private BeanInfo getBeanInfo(Class<?> claxx) throws IntrospectionException {
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
