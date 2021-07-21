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
public class JvmCloseCleaner {
    private final Logger log = LoggerFactory.getLogger(JvmCloseCleaner.class);
    private List<CloseableWrapper> closeableWrappers = new CopyOnWriteArrayList<>();
    private static final JvmCloseCleaner DEFAULT = new JvmCloseCleaner();

    public static final int MIN_PRIORITY = 1;
    public static final int MIDDLE_PRIORITY = 5;
    public static final int MAX_PRIORITY = 10;

    static {
        DEFAULT.waitingClose();
    }

    private JvmCloseCleaner() {
    }

    private void waitingClose() {
        //等spring容器完全初始化后执行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.closeableWrappers.sort(Comparator.comparingInt(CloseableWrapper::getNPriority));
            for (CloseableWrapper wrapper : this.closeableWrappers) {
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
        List<CloseableWrapper> wrappers = closeables.stream().map(c -> new CloseableWrapper(c, priority)).collect(Collectors.toList());
        this.closeableWrappers.addAll(wrappers);
    }

    public static JvmCloseCleaner DEFAULT() {
        return DEFAULT;
    }

    //---------------------------------------------------------------------------------------------------

    private class CloseableWrapper implements Closeable {
        private Closeable closeable;
        private int priority;

        public CloseableWrapper(Closeable closeable, int priority) {
            this.closeable = closeable;
            this.priority = priority;
        }

        public int getNPriority() {
            return -priority;
        }

        @Override
        public void close() {
            log.info("{} closing...", closeable.getClass().getSimpleName());
            long startTime = System.currentTimeMillis();
            closeable.close();
            long endTime = System.currentTimeMillis();
            log.info("{} close cost {} ms", closeable.getClass().getSimpleName(), endTime - startTime);
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
