package org.kin.framework.collection;

import org.springframework.lang.NonNull;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author huangjianqin
 * @date 2017/10/28
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> {
    private final ConcurrentHashMap<E, Boolean> items = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public Iterator<E> iterator() {
        return items.keySet().iterator();
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean contains(Object o) {
        return items.containsKey(o);
    }

    @Override
    @NonNull
    public Object[] toArray() {
        return items.keySet().toArray();
    }

    @Override
    @NonNull
    public <T> T[] toArray(T[] a) {
        return items.keySet().toArray(a);
    }

    @Override
    public boolean add(E e) {
        Boolean origin = items.put(e, Boolean.TRUE);
        return origin == null || origin;
    }

    @Override
    public boolean remove(Object o) {
        return items.remove(o);
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public String toString() {
        return items.keySet().toString();
    }

    @Override
    public Spliterator<E> spliterator() {
        return items.keySet().spliterator();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return items.keySet().removeIf(filter);
    }

    @Override
    public Stream<E> stream() {
        return items.keySet().stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return items.keySet().parallelStream();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        items.keySet().forEach(action);
    }

    /**
     * 浅复制
     */
    @Override
    public ConcurrentHashSet<E> clone() throws CloneNotSupportedException {
        super.clone();
        ConcurrentHashSet<E> cloned = new ConcurrentHashSet<>();
        cloned.addAll(items.keySet());
        return cloned;
    }
}
