package org.kin.framework.log.logback;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2017/11/14
 * 动态生成logback logger
 * 所有组件都必须start
 */
public class LogbackFactory {
    private ch.qos.logback.classic.Logger logbackLogger;

    public LogbackFactory(ch.qos.logback.classic.Logger logbackLogger) {
        this.logbackLogger = logbackLogger;
    }

    //---------------------------------------------------------------------------------------------

    public LogbackFactory add(Appender<ILoggingEvent> newAppender) {
        logbackLogger.addAppender(newAppender);
        return this;
    }

    @SafeVarargs
    public final LogbackFactory add(Appender<ILoggingEvent>... newAppenders) {
        for (Appender<ILoggingEvent> newAppender : newAppenders) {
            logbackLogger.addAppender(newAppender);
        }
        return this;
    }

    public LogbackFactory level(Level level) {
        logbackLogger.setLevel(level);
        return this;
    }

    public LogbackFactory additive(boolean additive) {
        logbackLogger.setAdditive(additive);
        return this;
    }

    public Logger get() {
        return logbackLogger;
    }

    //---------------------------------------------------------------------------------------------

    public static LogbackFactory create(String logggerName, LoggerContext context) {
        return new LogbackFactory(context.getLogger(logggerName));
    }

    public static LogbackFactory create(String logggerName) {
        org.slf4j.Logger logger = LoggerFactory.getLogger(logggerName);
        if (!ch.qos.logback.classic.Logger.class.isAssignableFrom(logger.getClass())) {
            throw new UnsupportedOperationException("only support logback");
        }

        return new LogbackFactory((ch.qos.logback.classic.Logger) logger);
    }

    public static Logger getAsyncRollingFileLogger(String basePath, AbstractLogEvent logEvent) {
        LoggerContext lc = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();

        ThresholdFilter infoFilter = new ThresholdFilter();
        infoFilter.setLevel("INFO");
        infoFilter.setContext(lc);
        infoFilter.start();

        TimeBasedRollingPolicy policy = new TimeBasedRollingPolicy();
        policy.setFileNamePattern(basePath + File.separator + "%d{yyyy-MM-dd}" + File.separator + logEvent.getFileName() + ".log.%d{yyyy-MM-dd}");
//        policy.setMaxHistory(30);
        policy.setContext(lc);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(lc);
        encoder.setPattern("[%p] %d{yyyy-MM-dd HH:mm:ss SSS} [%t] |  %C.%M\\(%L\\) : %msg%n%ex");
        encoder.start();

        RollingFileAppender<ILoggingEvent> dailyRollingFileAppender = new RollingFileAppender<>();
        dailyRollingFileAppender.setContext(lc);
        dailyRollingFileAppender.setName(logEvent.getAppenderName());
        dailyRollingFileAppender.addFilter(infoFilter);
        dailyRollingFileAppender.setRollingPolicy(policy);
        dailyRollingFileAppender.setEncoder(encoder);
        dailyRollingFileAppender.setAppend(true);

        //下面三行很关键
        policy.setParent(dailyRollingFileAppender);
        policy.start();
        dailyRollingFileAppender.start();

        AsyncAppender asyncAppender = new AsyncAppender();
        asyncAppender.setContext(lc);
        asyncAppender.setName(logEvent.getAsyncAppenderName());
        asyncAppender.addAppender(dailyRollingFileAppender);
        //包含调用者信息
        asyncAppender.setIncludeCallerData(true);
        asyncAppender.start();


        return LogbackFactory.create(logEvent.getLoggerName()).add(asyncAppender).level(Level.INFO).additive(false).get();
    }
}
