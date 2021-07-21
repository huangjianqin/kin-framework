package org.kin.framework.utils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

/**
 * 1、使用java调用控制台进程方式"Runtime.getRuntime().exec()"来运行脚本(shell或python)
 * 2、因为通过java调用控制台进程方式实现，需要保证目标机器PATH路径正确配置对应编译器
 * 3、脚本打印的日志存储在指定的日志文件上
 * 4、python 异常输出优先级高于标准输出，体现在Log文件中，因此推荐通过logging方式打日志保持和异常信息一致；否则用prinf日志顺序会错乱
 *
 * @author huangjianqin
 * @date 2020-02-19
 */
public class CommandUtils {
    private static final int NON_TIMEOUT = -1;

    /**
     * 启动进程
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 启动 结果
     */
    public static int execCommand(String command, String out, String workingDirectory, String... params) throws Exception {
        return execCommand(command, out, workingDirectory, NON_TIMEOUT, params);
    }

    /**
     * 启动进程
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 执行 结果
     */
    public static int execCommand(String command, String out) throws Exception {
        return execCommand(command, out, "", NON_TIMEOUT);
    }

    /**
     * 启动进程
     *
     * @return 返回进程 执行 结果
     */
    public static int execCommand(String command) throws Exception {
        return execCommand(command, "", "", NON_TIMEOUT);
    }

    /**
     * 启动进程
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 启动 结果
     */
    public static int execCommand(String command, String out, String workingDirectory, int timeout, String... params) throws Exception {
        File workingDirectoryFile = null;
        if (StringUtils.isNotBlank(workingDirectory)) {
            workingDirectoryFile = new File(workingDirectory);
            if (!workingDirectoryFile.exists()) {
                throw new IllegalArgumentException("directory '" + workingDirectory + "' not exists ");
            }
        }
        FileOutputStream fileOutputStream = null;

        try {
            PumpStreamHandler streamHandler;
            if (StringUtils.isNotBlank(out)) {
                fileOutputStream = new FileOutputStream(out, true);
                streamHandler = new PumpStreamHandler(fileOutputStream, fileOutputStream, null);
            } else {
                streamHandler = new PumpStreamHandler();
            }

            // command
            CommandLine commandline = new CommandLine(CommandLine.parse(command));
            if (params != null && params.length > 0) {
                commandline.addArguments(params);
            }

            //设置超时
            ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
            // executor
            DefaultExecutor executor = new DefaultExecutor();
            executor.setWatchdog(watchdog);
            executor.setStreamHandler(streamHandler);
            if (Objects.nonNull(workingDirectoryFile)) {
                executor.setWorkingDirectory(workingDirectoryFile);
            }
            return executor.execute(commandline);

//            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
//            executor.execute(commandline, resultHandler);
//            resultHandler.waitFor();
//            return resultHandler.getExitValue();
        } finally {
            if (Objects.nonNull(fileOutputStream)) {
                fileOutputStream.close();
            }
        }
    }
}
