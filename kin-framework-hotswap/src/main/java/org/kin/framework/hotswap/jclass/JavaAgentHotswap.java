package org.kin.framework.hotswap.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.classfile.ClassFile;
import org.kin.framework.utils.ExceptionUtils;
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
 * @author huangjianqin
 * @date 2018/2/3
 */
public final class JavaAgentHotswap implements org.kin.framework.hotswap.agent.JavaAgentHotswapMBean {
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
    /** 热加载过的class文件信息 */
    private final Map<String, org.kin.framework.hotswap.agent.ClassFileInfo> filePath2ClassFileInfo = new HashMap<>();

    static {
        CLASSPATH = SysUtils.getSysProperty("kin.hotswap.classpath", "hotswap/classes");
        log.info("java agent:classpath:{}", CLASSPATH);

        AGENT_PATH = SysUtils.getSysProperty("kin.hotswap.agent.dir", "hotswap/").concat("kin-java-agent.jar");
        log.info("java agent:jarPath:{}", AGENT_PATH);
    }

    /** 单例 */
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
            filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
        }

        File file = new File(filePath);

        filePath = file.getAbsolutePath();
        return filePath;
    }

    /**
     * 热更新逻辑
     */
    public synchronized boolean hotswap(List<Path> changedPaths) {
        //开始时间
        long startTime = System.currentTimeMillis();
        log.info("hotswap start...");
        try {
            Map<String, org.kin.framework.hotswap.agent.ClassFileInfo> newFilePath2ClassFileInfo = new HashMap<>(changedPaths.size());
            List<ClassDefinition> classDefList = new ArrayList<>(changedPaths.size());
            for (Path changedPath : changedPaths) {
                //class文件路径
                String classFilePath = changedPath.toString();
                try {
                    log.info("file '{}' checking...", classFilePath);
                    //原class文件信息
                    org.kin.framework.hotswap.agent.ClassFileInfo old = filePath2ClassFileInfo.get(classFilePath);
                    //过滤没有变化的文件(通过文件修改时间)
                    long classFileLastModifiedTime = Files.getLastModifiedTime(changedPath).toMillis();
                    if (old != null && old.getLastModifyTime() == classFileLastModifiedTime) {
                        log.info("file '{}' is ignored, because it's file modified time is not changed", classFilePath);
                        continue;
                    }

                    byte[] bytes = Files.readAllBytes(changedPath);

                    //从class文件字节码中读取className
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
                    ClassFile cf = ClassFile.read(dis);
                    String className = cf.getName().replaceAll("/", "\\.");
                    dis.close();

                    //封装成class文件信息
                    org.kin.framework.hotswap.agent.ClassFileInfo cfi = new org.kin.framework.hotswap.agent.ClassFileInfo(classFilePath, className, bytes, classFileLastModifiedTime);
                    //检查类名
                    if (old != null && !old.getClassName().equals(cfi.getClassName())) {
                        log.info("file '{}' is ignored, because it's class name is not the same with the origin", classFilePath);
                        continue;
                    }

                    //检查内容
                    if (old != null && !old.getMd5().equals(cfi.getMd5())) {
                        log.info("file '{}' is ignored, because it's content is not changed", classFilePath);
                        continue;
                    }

                    log.info("file '{}' pass check, it's class name is {}", classFilePath, className);
                    newFilePath2ClassFileInfo.put(classFilePath, cfi);

                    Class<?> c = Class.forName(className);
                    ClassDefinition classDefinition = new ClassDefinition(c, bytes);

                    classDefList.add(classDefinition);
                } catch (Exception e) {
                    throw new IllegalStateException(String.format("file '%s' parse error, hotswap fail", classFilePath), e);
                }
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
        } catch (UnmodifiableClassException | ClassNotFoundException e) {
            log.error("hotswap fail, due to", e);
        } finally {
            //结束时间
            long endTime = System.currentTimeMillis();
            log.info("...hotswap finish, cost {} ms", endTime - startTime);
        }

        return false;
    }

    @Override
    public List<org.kin.framework.hotswap.agent.ClassFileInfo> getClassFileInfo() {
        return new ArrayList<>(filePath2ClassFileInfo.values());
    }
}
