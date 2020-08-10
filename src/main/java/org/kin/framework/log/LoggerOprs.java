package org.kin.framework.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * Logger操作
 * <p>
 * 实现该接口, 可以轻松调用log, 不用再手写定义logger变量
 *
 * @author huangjianqin
 * @date 2020/8/10
 */
public interface LoggerOprs {
    default Logger log() {
        return LoggerFactory.getLogger(getClass());
    }

    default String name() {
        return log().getName();
    }

    //-------------------------------------------trace-------------------------------------------
    default void trace(String msg) {
        log().trace(msg);
    }

    default void trace(String msg, Object arg1) {
        log().trace(msg, arg1);
    }

    default void trace(String msg, Object arg1, Object arg2) {
        log().trace(msg, arg1, arg2);
    }

    default void trace(String format, Object... args) {
        log().trace(format, args);
    }

    default void trace(Marker marker, String format, Object... args) {
        log().trace(marker, format, args);
    }

    default void trace(String msg, Throwable t) {
        log().trace(msg, t);
    }

    //-------------------------------------------debug-------------------------------------------
    default void debug(String msg) {
        log().debug(msg);
    }

    default void debug(String msg, Object arg1) {
        log().debug(msg, arg1);
    }

    default void debug(String msg, Object arg1, Object arg2) {
        log().debug(msg, arg1, arg2);
    }

    default void debug(String format, Object... args) {
        log().debug(format, args);
    }

    default void debug(Marker marker, String format, Object... args) {
        log().debug(marker, format, args);
    }

    default void debug(String msg, Throwable t) {
        log().debug(msg, t);
    }

    default void debug(Throwable t) {
        log().debug("", t);
    }

    //-------------------------------------------info-------------------------------------------
    default void info(String msg) {
        log().info(msg);
    }

    default void info(String msg, Object arg1) {
        log().info(msg, arg1);
    }

    default void info(String msg, Object arg1, Object arg2) {
        log().info(msg, arg1, arg2);
    }

    default void info(String format, Object... args) {
        log().info(format, args);
    }

    default void info(Marker marker, String format, Object... args) {
        log().info(marker, format, args);
    }

    default void info(String msg, Throwable t) {
        log().info(msg, t);
    }

    //-------------------------------------------warn-------------------------------------------
    default void warn(String msg) {
        log().warn(msg);
    }

    default void warn(String msg, Object arg1) {
        log().warn(msg, arg1);
    }

    default void warn(String msg, Object arg1, Object arg2) {
        log().warn(msg, arg1, arg2);
    }

    default void warn(String format, Object... args) {
        log().warn(format, args);
    }

    default void warn(Marker marker, String format, Object... args) {
        log().warn(marker, format, args);
    }

    default void warn(String msg, Throwable t) {
        log().warn(msg, t);
    }

    //-------------------------------------------error-------------------------------------------
    default void error(String msg) {
        log().error(msg);
    }

    default void error(String msg, Object arg1) {
        log().error(msg, arg1);
    }

    default void error(String msg, Object arg1, Object arg2) {
        log().error(msg, arg1, arg2);
    }

    default void error(String format, Object... args) {
        log().error(format, args);
    }

    default void error(Marker marker, String format, Object... args) {
        log().error(marker, format, args);
    }

    default void error(String msg, Throwable t) {
        log().error(msg, t);
    }
}
