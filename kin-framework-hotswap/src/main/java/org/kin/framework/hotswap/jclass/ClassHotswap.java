package org.kin.framework.hotswap.jclass;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import org.kin.agent.JavaDynamicAgent;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.management.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author huangjianqin
 * @date 2018/2/3
 */
public final class ClassHotswap implements ClassHotswapMBean {
    private static final Logger log = LoggerFactory.getLogger(ClassHotswap.class);
    /** jar包后缀 */
    private static final String JAR_SUFFIX = ".jar";
    /** class文件后缀 */
    private static final String CLASS_SUFFIX = ".class";
    /** zip压缩包后缀 */
    private static final String ZIP_SUFFIX = ".zip";
    /**
     * 热更class文件放另外一个目录
     * 开发者指定, 也可以走配置
     */
    public static final String CLASSPATH;
    /**
     * java agent jar路径
     */
    public static final String AGENT_PATH;
    /** 热加载过的class文件信息, key -> class name */
    private final Map<String, ClassFileInfo> name2ClassFileInfo = new HashMap<>();

    static {
        CLASSPATH = SysUtils.getSysProperty("kin.hotswap.classpath", "hotswap/classes");
        log.info("java agent:classpath:{}", CLASSPATH);

        AGENT_PATH = SysUtils.getSysProperty("kin.hotswap.agent.dir", "hotswap/").concat("kin-java-agent.jar");
        log.info("java agent:jarPath:{}", AGENT_PATH);
    }

    /** 单例 */
    private static ClassHotswap INSTANCE;

    public static ClassHotswap instance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (ClassHotswap.class) {
                if (Objects.nonNull(INSTANCE)) {
                    return INSTANCE;
                }
                INSTANCE = new ClassHotswap();
            }
        }
        return INSTANCE;
    }

    private ClassHotswap() {
        initMBean();
    }

    /**
     * 初始化JMX监控
     */
    private void initMBean() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(this + ":type=JavaAgentHotswap");
            mBeanServer.registerMBean(this, name);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 热更新逻辑
     */
    public synchronized boolean hotswap(List<Path> changedPaths) {
        //开始时间
        long startTime = System.currentTimeMillis();
        log.info("hotswap start...");
        try {
            Map<String, ClassFileInfo> newName2ClassFileInfo = new HashMap<>(changedPaths.size());
            //加载到的待热更新的class定义
            List<ClassDefinition> classDefinitions = new ArrayList<>(changedPaths.size());
            ByteArrayOutputStream baos = null;
            for (Path changedPath : changedPaths) {
                if (Files.isDirectory(changedPath) ||
                        Files.isHidden(changedPath) ||
                        !Files.isReadable(changedPath)) {
                    //过滤目录, 隐藏文件, 不可读文件
                    continue;
                }

                String changedFileName = changedPath.getFileName().toString();
                if (!changedFileName.endsWith(CLASS_SUFFIX) && !changedFileName.endsWith(ZIP_SUFFIX)) {
                    //只允许.class和.zip
                    continue;
                }

                //文件路径
                String filePath = changedPath.toString();
                long fileLastModifiedMs = Files.getLastModifiedTime(changedPath).toMillis();
                try {
                    if (changedFileName.endsWith(ZIP_SUFFIX)) {
                        if (Objects.isNull(baos)) {
                            //default 64k
                            baos = new ByteArrayOutputStream(65536);
                        } else {
                            //复用前先reset
                            baos.reset();
                        }
                        classDefinitions.addAll(parseZip(changedPath, baos, newName2ClassFileInfo));
                    } else {
                        byte[] bytes = Files.readAllBytes(changedPath);
                        ClassDefinition classDefinition = toClassDefinition(filePath, fileLastModifiedMs, bytes, newName2ClassFileInfo);
                        if (Objects.nonNull(classDefinition)) {
                            classDefinitions.add(classDefinition);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(String.format("file '%s' parse error, hotswap fail", filePath), e);
                } finally {
                    if (Objects.nonNull(baos)) {
                        baos.close();
                    }
                }
            }
            if (Objects.nonNull(baos)) {
                baos.close();
            }

            // 当前进程pid
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.split("@")[0];
            log.debug("now pid is '{}'", pid);

            // 虚拟机加载
            VirtualMachine vm = null;
            try {
                vm = VirtualMachine.attach(pid);
                //JavaDynamicAgent所在的jar包
                //app jar包与agent jar包同一路径
                vm.loadAgent(AGENT_PATH);

                //重新定义类
                JavaDynamicAgent.getInstrumentation().redefineClasses(classDefinitions.toArray(new ClassDefinition[0]));

                //更新元数据
                name2ClassFileInfo.putAll(newName2ClassFileInfo);

                //删除热更类文件
                Path rootPath = Paths.get(CLASSPATH);
                Files.list(rootPath).forEach(childpath -> {
                    try {
                        Files.deleteIfExists(childpath);
                    } catch (IOException e) {
                        ExceptionUtils.throwExt(e);
                    }
                });
                return true;
            } catch (AttachNotSupportedException | AgentLoadException | AgentInitializationException | IOException e) {
                log.error("hotswap fail, due to", e);
            } finally {
                if (vm != null) {
                    try {
                        vm.detach();
                    } catch (IOException e) {
                        ExceptionUtils.throwExt(e);
                    }
                }
            }
        } catch (UnmodifiableClassException | ClassNotFoundException | IOException e) {
            log.error("hotswap fail, due to", e);
        } finally {
            //结束时间
            long endTime = System.currentTimeMillis();
            log.info("...hotswap finish, cost {} ms", endTime - startTime);
        }

        return false;
    }

    /**
     * 根据规则过滤并将合法的class文件内容转换成{@link ClassDefinition}实例
     *
     * @param classFilePath           class文件路径
     * @param classFileLastModifiedMs class文件上次修改时间
     * @param bytes                   class文件内容
     * @param newName2ClassFileInfo   新的热加载过的class文件信息
     * @return class定义
     */
    @Nullable
    private ClassDefinition toClassDefinition(String classFilePath, long classFileLastModifiedMs,
                                              byte[] bytes, Map<String, ClassFileInfo> newName2ClassFileInfo) throws ClassNotFoundException, ConstantPoolException, IOException {
        log.info("file '{}' checking...", classFilePath);

        //从class文件字节码中读取className
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        ClassFile cf = ClassFile.read(dis);
        String className = cf.getName().replaceAll("/", "\\.");
        dis.close();

        //原class文件信息
        ClassFileInfo old = name2ClassFileInfo.get(className);
        //过滤没有变化的文件(通过文件修改时间)
        if (old != null && old.getLastModifyTime() == classFileLastModifiedMs) {
            log.info("file '{}' is ignored, because it's file modified time is not changed", classFilePath);
            return null;
        }

        //封装成class文件信息
        ClassFileInfo cfi = new ClassFileInfo(classFilePath, className, bytes, classFileLastModifiedMs);
        //检查类名
        if (old != null && !old.getClassName().equals(cfi.getClassName())) {
            log.info("file '{}' is ignored, because it's class name is not the same with the origin", classFilePath);
            return null;
        }

        //检查内容
        if (old != null && !old.getMd5().equals(cfi.getMd5())) {
            log.info("file '{}' is ignored, because it's content is not changed", classFilePath);
            return null;
        }

        log.info("file '{}' pass check, it's class name is {}", classFilePath, className);
        newName2ClassFileInfo.put(className, cfi);

        Class<?> c = Class.forName(className);
        return new ClassDefinition(c, bytes);
    }

    /**
     * 解析zip包, 该zip包可能包含多个class文件.
     * 之所以需要打包成zip, 因为想批量redefine, 这样子可以保证同时热更新成功, 或者同时热更新失败, 不会污染运行时环境
     * 不打包成zip, 有可能因为网络传输延迟, 想要热更新的class文件, 分批到达, 这样子框架会认为是多次热更新, 这样子无法达到预期效果, 还很有可能污染运行时环境
     *
     * @param changedPath           包含class文件的zip路径
     * @param baos                  复用的{@link ByteArrayOutputStream}, 单线程操作, 复用可以减少内存分配
     * @param newName2ClassFileInfo 新的热加载过的class文件信息
     * @return 该zip包含的所有class定义
     */
    private List<ClassDefinition> parseZip(Path changedPath, ByteArrayOutputStream baos, Map<String, ClassFileInfo> newName2ClassFileInfo) throws IOException, ConstantPoolException, ClassNotFoundException {
        String separator = changedPath.getFileSystem().getSeparator();
        //模拟uri的路径格式
        String zipFilePath = changedPath + "!" + separator;
        List<ClassDefinition> classDefinitions = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(changedPath))) {
            ZipEntry entry;
            while (Objects.nonNull((entry = zis.getNextEntry()))) {
                if (entry.isDirectory()) {
                    //过滤目录
                    zis.closeEntry();
                    continue;
                }

                String fileName = entry.getName();
                if (!fileName.endsWith(CLASS_SUFFIX)) {
                    //过滤非class文件
                    zis.closeEntry();
                    continue;
                }

                String classFilePath = zipFilePath + fileName;
                //获取文件修改时间
                long classFileLastModifiedMs = entry.getLastModifiedTime().toMillis();

                //读取class文件内容
                int len;
                byte[] buffer = new byte[1024];
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }

                //转换成ClassDefinition
                ClassDefinition classDefinition = toClassDefinition(classFilePath, classFileLastModifiedMs, baos.toByteArray(), newName2ClassFileInfo);
                if (Objects.nonNull(classDefinition)) {
                    classDefinitions.add(classDefinition);
                }

                zis.closeEntry();
                //重置
                baos.reset();
            }
        }

        return classDefinitions;
    }

    @Override
    public List<ClassFileInfo> getClassFileInfo() {
        return new ArrayList<>(name2ClassFileInfo.values());
    }
}
