package org.kin.framework.utils;

import java.util.*;

/**
 * 外部extension class instance cache
 *
 * @author huangjianqin
 * @date 2021/11/21
 * @see ExtensionLoader
 */
public abstract class AbstractExtensionCache<K, S> {
    /** key-> class name, value -> extension class instance */
    protected final Map<K, S> cache;
    /** extension class */
    private final Class<S> extensionClass;

    @SuppressWarnings("unchecked")
    public AbstractExtensionCache() {
        List<Class<?>> classes = ClassUtils.getSuperClassGenericRawTypes(getClass());
        extensionClass = (Class<S>) classes.get(1);

        //init load
        Map<K, S> cache = new HashMap<>(4);
        //通过spi机制加载自定义的extension class instance
        List<S> extensions = ExtensionLoader.getExtensions(extensionClass);
        for (S extension : extensions) {
            Class<?> claxx = extension.getClass();
            K[] keys = keys(extension);
            for (K key : keys) {
                cache.put(key, extension);
            }
        }
        this.cache = Collections.unmodifiableMap(cache);
    }

    private String getExtensionSimpleName() {
        return extensionClass.getSimpleName();
    }

    /**
     * 目标extension转换成指定key的逻辑, 支持同一类型多个key
     */
    protected abstract K[] keys(S extension);

    /**
     * 根据key获取extension class instance
     */
    public S getExtension(K key) {
        if (Objects.isNull(key)) {
            throw new IllegalArgumentException(String.format("%s key is null", getExtensionSimpleName()));
        }

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        throw new IllegalArgumentException(String.format("unable to load %s for key '%s'", getExtensionSimpleName(), key));
    }
}