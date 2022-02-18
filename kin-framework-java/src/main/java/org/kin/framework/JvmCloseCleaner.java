package org.kin.framework;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 用于控制jvm close时, 释放占用资源
 *
 * @author huangjianqin
 * @date 2019/2/28
 */
public final class JvmCloseCleaner {
    private final Logger log = LoggerFactory.getLogger(JvmCloseCleaner.class);

    /** 最低优先级 */
    public static final int MIN_PRIORITY = 1;
    /** 一般优先级 */
    public static final int MIDDLE_PRIORITY = 5;
    /** 最高优先级 */
    public static final int MAX_PRIORITY = 10;

    private static final JvmCloseCleaner INSTANCE = new JvmCloseCleaner();

    private final List<DelegatingOrderedCloseable> orderedCloseableList = new CopyOnWriteArrayList<>();

    private JvmCloseCleaner() {
        waitingClose();
    }

    private void waitingClose() {
        //等spring容器完全初始化后执行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.orderedCloseableList.sort(Comparator.comparingInt(DelegatingOrderedCloseable::getNPriority));
            for (DelegatingOrderedCloseable wrapper : this.orderedCloseableList) {
                wrapper.close();
            }
        }));
    }

    public void add(Closeable closeable) {
        add(MIN_PRIORITY, closeable);

    }

    public void addAll(Closeable... closeables) {
        addAll(MIN_PRIORITY, Arrays.asList(closeables));
    }

    public void addAll(Collection<Closeable> closeables) {
        addAll(MIN_PRIORITY, closeables);
    }

    public void add(int priority, Closeable closeable) {
        addAll(priority, closeable);
    }

    public void addAll(int priority, Closeable... closeables) {
        addAll(priority, Arrays.asList(closeables));
    }

    public void addAll(int priority, Collection<Closeable> closeables) {
        Preconditions.checkArgument(0 < priority && priority <= 10, "priority must be range from 1 to 10");
        List<DelegatingOrderedCloseable> orderedCloseableList = closeables.stream().map(c -> new DelegatingOrderedCloseable(c, priority)).collect(Collectors.toList());
        this.orderedCloseableList.addAll(orderedCloseableList);
    }

    public static JvmCloseCleaner instance() {
        return INSTANCE;
    }

    //---------------------------------------------------------------------------------------------------

    /**
     * 支持带优先级的且委托其他{@link Closeable}执行close逻辑的{@link Closeable}实例
     */
    private class DelegatingOrderedCloseable implements Closeable {
        /** 真正的{@link Closeable}实例 */
        private final Closeable closeable;
        /** 优先级 */
        private final int priority;

        public DelegatingOrderedCloseable(Closeable closeable, int priority) {
            this.closeable = closeable;
            this.priority = priority;
        }

        /**
         * 获取取反后的优先级
         */
        public int getNPriority() {
            return -priority;
        }

        @Override
        public void close() {
            log.info("{} closing...", closeable.getClass().getName());
            long startTime = System.currentTimeMillis();
            closeable.close();
            long endTime = System.currentTimeMillis();
            log.info("{} close cost {} ms", closeable.getClass().getName(), endTime - startTime);
        }

        //getter
        public Closeable getCloseable() {
            return closeable;
        }

        public int getPriority() {
            return priority;
        }
    }
}
