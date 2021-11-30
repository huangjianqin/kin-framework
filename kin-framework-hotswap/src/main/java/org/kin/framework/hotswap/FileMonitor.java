package org.kin.framework.hotswap;

import org.kin.framework.Closeable;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.framework.hotswap.agent.JavaAgentHotswap;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件监听器   单例模式
 * 利用nio 新api监听文件变换
 * 该api底层本质上是监听了操作系统的文件系统触发的文件更改事件
 * <p>
 * 异步热加载文件 同步类热更新
 *
 * @author huangjianqin
 * @date 2018/2/1
 */
public class FileMonitor extends Thread implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(FileMonitor.class);
    /** 文件变化监听服务, 基于文件系统事件触发 */
    private WatchService watchService;
    /** hash(file name) -> Reloadable 实例 */
    private Map<Integer, AbstractFileReloadable> monitorItems;
    /** 异步热加载文件以及类热更新执行线程 */
    private ExecutionContext executionContext;
    private volatile boolean isStopped = false;
    /** 热更新listeners */
    private final List<HotswapListener> listeners = ExtensionLoader.common().getExtensions(HotswapListener.class);

    public FileMonitor() {
        this("hotSwapFileMonitor", null);
    }

    public FileMonitor(String name) {
        this(name, null);
    }

    public FileMonitor(ExecutionContext executionContext) {
        this("hotSwapFileMonitor", executionContext);
    }

    public FileMonitor(String name, ExecutionContext executionContext) {
        super(name);
        this.executionContext = executionContext;
    }

    private void init() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();

        monitorItems = new ConcurrentHashMap<>();
        if (executionContext == null) {
            //默认设置
            this.executionContext = ExecutionContext.elastic(0, SysUtils.CPU_NUM, "file-monitor");
        }

        //监听热更class存储目录
        Path classesPath = Paths.get(JavaAgentHotswap.CLASSPATH);
        classesPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        monitorJVMClose();
    }

    @Override
    public synchronized void start() {
        try {
            init();
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }
        super.start();
    }

    @Override
    public void run() {
        log.info("file monitor start");
        while (!isStopped && !Thread.currentThread().isInterrupted()) {
            List<Path> changedClasses = new ArrayList<>();
            try {
                WatchKey key = watchService.take();
                //变化的路径
                Path parentPath = (Path) key.watchable();
                List<WatchEvent<?>> events = key.pollEvents();
                events.forEach(event -> {
                    //变化item的名字(文件名或者文件夹名)
                    String itemName = event.context().toString();
                    int hashKey = itemName.hashCode();
                    //真实路径
                    Path childPath = Paths.get(parentPath.toString(), itemName);
                    log.debug("'{}' changed", childPath);
                    if (!Files.isDirectory(childPath)) {
                        //非文件夹
                        if (itemName.endsWith(ClassUtils.CLASS_SUFFIX)) {
                            //处理类热更新
                            changedClasses.add(childPath);
                        } else {
                            //处理文件热更新
                            AbstractFileReloadable fileReloadable = monitorItems.get(hashKey);
                            if (fileReloadable != null) {
                                executionContext.execute(() -> {
                                    try {
                                        long startTime = System.currentTimeMillis();
                                        try (InputStream is = new FileInputStream(childPath.toFile())) {
                                            fileReloadable.reload(is);
                                        }
                                        long endTime = System.currentTimeMillis();
                                        log.info("hotswap file '{}' finished, time cost {} ms", childPath, endTime - startTime);
                                    } catch (IOException e) {
                                        log.error("", e);
                                    }
                                });
                            }
                        }
                    }
                });
                //重置状态，让key等待事件
                key.reset();
            } catch (InterruptedException e) {
                //do nothing
            }

            if (changedClasses.size() > 0) {
                //类热更新
                executionContext.execute(() -> {
                    if (JavaAgentHotswap.instance().hotswap(changedClasses)) {
                        //延迟5s执行
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                for (HotswapListener listener : listeners) {
                                    try {
                                        listener.afterHotswap();
                                    } catch (Exception e) {
                                        log.error("encounter error, when trigger HotswapListener", e);
                                    }
                                }
                            }
                        }, 5 * 1000);
                    }
                });
            }
        }
        log.info("file monitor end");
    }

    public void shutdown() {
        if (!isStopped) {
            isStopped = true;
            try {
                watchService.close();
            } catch (IOException e) {
                ExceptionUtils.throwExt(e);
            }
            executionContext.shutdown();
            //help GC
            monitorItems = null;

            //中断监控线程, 让本线程退出
            interrupt();
        }
    }

    //--------------------------------------------------API---------------------------------------------------------------
    private void checkStatus() {
        if (isStopped) {
            throw new IllegalStateException("file monitor has shutdowned");
        }
    }

    public void monitorFile(String pathStr, AbstractFileReloadable fileReloadable) {
        checkStatus();
        Path path = Paths.get(pathStr);
        monitorFile(path, fileReloadable);
    }

    public void monitorFile(Path path, AbstractFileReloadable fileReloadable) {
        checkStatus();
        if (!Files.isDirectory(path)) {
            try {
                monitorFile0(path.getParent(), path.getFileName().toString(), fileReloadable);
            } catch (IOException e) {
                ExceptionUtils.throwExt(e);
            }
        } else {
            throw new IllegalStateException("monitor file dir error");
        }
    }

    /**
     * 监听文件变化
     */
    private void monitorFile0(Path file, String itemName, AbstractFileReloadable fileReloadable) throws IOException {

        int key = itemName.hashCode();
        AbstractFileReloadable old = monitorItems.putIfAbsent(key, fileReloadable);
        if (Objects.nonNull(old)) {
            throw new IllegalStateException(String.format("file '%s' has been monitored", file));
        }
        file.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
    }

    @Override
    public void close() {
        shutdown();
    }
}
