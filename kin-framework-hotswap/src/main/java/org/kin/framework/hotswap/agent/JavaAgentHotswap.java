package org.kin.framework.hotswap.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.Symbols;
import org.kin.framework.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 单例模式
 *
 * @author huangjianqin
 * @date 2018/2/3
 */
public final class JavaAgentHotswap implements JavaAgentHotswapMBean {
    private static final Logger log = LoggerFactory.getLogger(JavaAgentHotswap.class);
    /** jar包后缀 */
    private static final String JAR_SUFFIX = ".jar";
    /**
     * 热更class文件放另外一个目录
     * 开发者指定, 也可以走配置
     */
    public static final String CLASSPATH;
    /**
     * java agent jar路径
     */
    public static final String AGENT_PATH;
    private volatile boolean isInit;
    private final Map<String, ClassFileInfo> filePath2ClassFileInfo = new HashMap<>();

    static {
        CLASSPATH = SysUtils.getSysProperty("kin.hotswap.classpath", "hotswap/classes");
        log.info("java agent:classpath:{}", CLASSPATH);

        AGENT_PATH = SysUtils.getSysProperty("kin.hotswap.agent.dir", "hotswap/").concat("KinJavaAgent.jar");
        log.info("java agent:jarPath:{}", AGENT_PATH);
    }

    private static JavaAgentHotswap INSTANCE;

    public static JavaAgentHotswap instance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (JavaAgentHotswap.class) {
                if (Objects.nonNull(INSTANCE)) {
                    return INSTANCE;
                }
                INSTANCE = new JavaAgentHotswap();
            }
        }
        return INSTANCE;
    }

    private JavaAgentHotswap() {
        init();
    }

    private void init() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(this + ":type=JavaAgentHotswap");
            mBeanServer.registerMBean(this, name);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException | MBeanRegistrationException e) {
            ExceptionUtils.throwExt(e);
        }
    }

    /**
     * 获取jar包路径
     */
    private static String getAgentPath() {
        //JavaDynamicAgent是jar文件内容,也就是说jar必须包含JavaDynamicAgent
        URL url = JavaDynamicAgent.class.getProtectionDomain().getCodeSource().getLocation();
        String filePath;
        try {
            // 转化为utf-8编码
            filePath = URLDecoder.decode(url.getPath(), "utf-8");
        } catch (Exception e) {
            ExceptionUtils.throwExt(e);
            //理论上不会到这里
            throw new IllegalStateException("encounter unknown error");
        }
        // 可执行jar包运行的结果里包含".jar"
        if (filePath.endsWith(JAR_SUFFIX)) {
            // 截取路径中的jar包名
            filePath = filePath.substring(0, filePath.lastIndexOf(Symbols.DIVIDE) + 1);
        }

        File file = new File(filePath);

        filePath = file.getAbsolutePath();
        return filePath;
    }

    /**
     * 热更逻辑
     */
    public synchronized boolean hotswap(List<Path> changedPaths) {
        long startTime = System.currentTimeMillis();
        log.info("开始热更类...");
        try {
            Map<String, ClassFileInfo> newFilePath2ClassFileInfo = new HashMap<>(changedPaths.size());
            List<ClassDefinition> classDefList = new ArrayList<>(changedPaths.size());
            for (Path changedPath : changedPaths) {
                String classFilePath = changedPath.toString();
                ClassFileInfo old = filePath2ClassFileInfo.get(classFilePath);
                //过滤没有变化的文件(通过文件修改时间)
                long classFileLastModifiedTime = Files.getLastModifiedTime(changedPath).toMillis();
                if (old == null || old.getLastModifyTime() != classFileLastModifiedTime) {
                    log.info("开始检查文件'{}'", classFilePath);
                    boolean checkClassName = false;
                    byte[] bytes = Files.readAllBytes(changedPath);

                    //从class文件字节码中读取className
                    String className = null;
                    try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
                        ClassFile cf = ClassFile.read(dis);
                        className = cf.getName().replaceAll(Symbols.DIVIDE, "\\.");
                    } catch (IOException | ConstantPoolException e) {
                        ExceptionUtils.throwExt(e);
                    }

                    ClassFileInfo cfi = new ClassFileInfo(classFilePath, className, bytes, classFileLastModifiedTime);
                    //检查类名
                    if (old == null || old.getClassName().equals(cfi.getClassName())) {
                        log.info("文件'{}' 类'{}'检查成功", classFilePath, className);
                        checkClassName = true;
                        newFilePath2ClassFileInfo.put(classFilePath, cfi);
                    }

                    if (checkClassName) {
                        Class<?> c = Class.forName(className);
                        ClassDefinition classDefinition = new ClassDefinition(c, bytes);

                        classDefList.add(classDefinition);
                    } else {
                        throw new IllegalStateException("因为文件 '" + classFilePath + "' 解析失败, 故热更失败");
                    }
                }
            }

            // 当前进程pid
            String name = ManagementFactory.getRuntimeMXBean().getName();
            String pid = name.split("@")[0];
            log.debug("当前进程pid：{}", pid);

            // 虚拟机加载
            VirtualMachine vm = null;
            try {
                vm = VirtualMachine.attach(pid);
                //JavaDynamicAgent所在的jar包
                //app jar包与agent jar包同一路径
                vm.loadAgent(AGENT_PATH);

                //重新定义类
                JavaDynamicAgent.getInstrumentation().redefineClasses(classDefList.toArray(new ClassDefinition[0]));

                //更新元数据
                filePath2ClassFileInfo.putAll(newFilePath2ClassFileInfo);

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
                log.error("热更失败", e);
            } finally {
                if (vm != null) {
                    try {
                        vm.detach();
                    } catch (IOException e) {
                        ExceptionUtils.throwExt(e);
                    }
                }
            }
        } catch (IOException | UnmodifiableClassException | ClassNotFoundException e) {
            log.error("热更失败", e);
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("...热更类结束, 耗时 {} ms", endTime - startTime);
        }

        return false;
    }

    @Override
    public List<ClassFileInfo> getClassFileInfo() {
        return new ArrayList<>(filePath2ClassFileInfo.values());
    }
}
