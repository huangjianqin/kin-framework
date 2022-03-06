package org.kin.framework.collection;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExtensionLoader;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * map工厂类管理
 *
 * @author huangjianqin
 * @date 2021/12/18
 */
public class MapFactories extends AbstractFactories<MapFactory<?>> {
    /** 单例 */
    private static final MapFactories INSTANCE = new MapFactories();

    public static MapFactories instance() {
        return INSTANCE;
    }

    private MapFactories() {
        Map<Class<?>, MapFactory<?>> factories = new HashMap<>();

        //内置
        factories.put(Map.class, HashMap::new);
        factories.put(SortedMap.class, TreeMap::new);
        factories.put(NavigableMap.class, TreeMap::new);
        factories.put(HashMap.class, HashMap::new);
        factories.put(LinkedHashMap.class, LinkedHashMap::new);
        factories.put(TreeMap.class, TreeMap::new);
        factories.put(WeakHashMap.class, WeakHashMap::new);
        factories.put(IdentityHashMap.class, IdentityHashMap::new);
        factories.put(Hashtable.class, Hashtable::new);
        factories.put(ConcurrentMap.class, ConcurrentHashMap::new);
        factories.put(ConcurrentHashMap.class, ConcurrentHashMap::new);
        factories.put(ConcurrentNavigableMap.class, ConcurrentSkipListMap::new);
        factories.put(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new);
        factories.put(Properties.class, Properties::new);

        //通过spi加载
        for (MapFactory<?> factory : ExtensionLoader.getExtensions(MapFactory.class)) {
            List<Type> actualTypes = ClassUtils.getSuperInterfacesGenericActualTypes(MapFactory.class, factory.getClass());
            factories.put((Class<?>) actualTypes.get(0), factory);
        }

        register(factories);
    }

    @Override
    public synchronized void register(Class<?> type, MapFactory<?> factory) {
        if (!Map.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format("type '%s' is not a map", type.getCanonicalName()));
        }
        super.register(type, factory);
    }
}
