package org.kin.framework.collection;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExtensionLoader;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.*;
import java.util.concurrent.*;

/**
 * 集合工厂类管理
 *
 * @author huangjianqin
 * @date 2021/12/18
 */
public class CollectionFactories extends AbstractFactories<CollectionFactory<?>> {
    /** 单例 */
    private static final CollectionFactories INSTANCE = new CollectionFactories();

    public static CollectionFactories instance() {
        return INSTANCE;
    }

    private CollectionFactories() {
        Map<Class<?>, CollectionFactory<?>> factories = new HashMap<>();

        //内置
        factories.put(Collection.class, ArrayList::new);
        factories.put(List.class, ArrayList::new);
        factories.put(ArrayList.class, ArrayList::new);
        factories.put(LinkedList.class, LinkedList::new);
        factories.put(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);
        factories.put(Stack.class, Stack::new);
        factories.put(Vector.class, Vector::new);
        factories.put(Set.class, HashSet::new);
        factories.put(HashSet.class, HashSet::new);
        factories.put(LinkedHashSet.class, LinkedHashSet::new);
        factories.put(SortedSet.class, TreeSet::new);
        factories.put(NavigableSet.class, TreeSet::new);
        factories.put(TreeSet.class, TreeSet::new);
        factories.put(Queue.class, LinkedList::new);
        factories.put(BlockingQueue.class, LinkedBlockingQueue::new);
        factories.put(Deque.class, LinkedBlockingDeque::new);
        factories.put(BlockingDeque.class, LinkedList::new);
        factories.put(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new);
        factories.put(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new);
        factories.put(LinkedBlockingQueue.class, LinkedBlockingQueue::new);
        factories.put(LinkedBlockingDeque.class, LinkedBlockingDeque::new);
        factories.put(ArrayBlockingQueue.class, () -> new ArrayBlockingQueue<>(10));
        factories.put(java.util.ArrayDeque.class, ArrayDeque::new);
        factories.put(ConcurrentLinkedQueue.class, ConcurrentLinkedQueue::new);
        factories.put(ConcurrentLinkedDeque.class, ConcurrentLinkedDeque::new);
        factories.put(PriorityBlockingQueue.class, PriorityBlockingQueue::new);
        factories.put(java.util.PriorityQueue.class, PriorityQueue::new);

        //通过spi加载
        for (CollectionFactory<?> factory : ExtensionLoader.getExtensions(CollectionFactory.class)) {
            List<Type> actualTypes = ClassUtils.getSuperInterfacesGenericActualTypes(CollectionFactory.class, factory.getClass());
            factories.put((Class<?>) actualTypes.get(0), factory);
        }

        register(factories);
    }

    @Override
    public synchronized void register(Class<?> type, CollectionFactory<?> factory) {
        if (!Collection.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(String.format("type '%s' is not a collection", type.getCanonicalName()));
        }
        super.register(type, factory);
    }
}
