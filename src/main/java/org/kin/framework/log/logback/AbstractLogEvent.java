package org.kin.framework.log.logback;

import org.kin.framework.utils.StringUtils;

/**
 * Created by huangjianqin on 2017/11/14.
 * 临时用于对某事件进行log,而不用特意修改配置文件
 */
public abstract class AbstractLogEvent {
    protected String fileName;
    protected String loggerName;
    protected String appenderName;

    /**
     * @return 日志输入内容
     */
    public abstract String message();

    public String getFileName() {
        if (StringUtils.isBlank(fileName)) {
            String className = getClass().getSimpleName();
            int lastIndex = className.lastIndexOf("LogEvent");
            fileName = lastIndex == -1 ? className : className.substring(0, lastIndex);
        }
        return fileName;
    }

    public String getLoggerName() {
        if (StringUtils.isBlank(loggerName)) {
            loggerName = getFileName() + "Logger";
        }
        return loggerName;
    }

    public String getAppenderName() {
        if (StringUtils.isBlank(appenderName)) {
            appenderName = getLoggerName() + "Appender";
        }
        return appenderName;
    }

    public String getAsyncAppenderName() {
        if (StringUtils.isBlank(appenderName)) {
            return "async" + getAppenderName();
        }
        return "async" + appenderName;
    }

    /**
     * debug输出
     */
    public abstract void debug();

    /**
     * info输出
     */
    public abstract void info();

    /**
     * warn输出
     */
    public abstract void warn();

    /**
     * error输出
     */
    public abstract void error();
}
