package org.kin.framework.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2018/2/2
 */
public class TimeUtils {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //-------------------------------------------------------get------------------------------------------------------

    /**
     * 由{@link System#nanoTime()}转换成milliseconds
     */
    public static long millisFromNanoTime() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    /**
     * 返回时间戳
     *
     * @return 时间戳
     */
    public static int timestamp() {
        return (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }

    /**
     * 返回 代表'年月日' 的 DateFormat
     */
    public static DateTimeFormatter getDateFormat() {
        return DATE_FORMAT;
    }

    /**
     * 返回 代表'年月日时分秒' 的 DateFormat
     */
    public static DateTimeFormatter getDateTimeFormat() {
        return DATETIME_FORMAT;
    }

    //-------------------------------------------------------format or parse------------------------------------------------------

    /**
     * 格式化当前时间
     *
     * @return 时间字符串(年月日)
     */
    public static String formatDate() {
        return formatDate(LocalDate.now());
    }

    /**
     * 格式化时间
     *
     * @param date 时间
     * @return 时间字符串(年月日)
     */
    public static String formatDate(LocalDate date) {
        return date.format(getDateFormat());
    }

    /**
     * 格式化当前时间
     *
     * @return 时间字符串(年月日时分秒)
     */
    public static String formatDateTime() {
        return formatDateTime(LocalDateTime.now());
    }

    /**
     * 格式化时间
     *
     * @param dateTime 时间
     * @return 时间字符串(年月日时分秒)
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(getDateTimeFormat());
    }

    /**
     * 解析时间(年月日)
     *
     * @param dateString 时间字符串
     * @return 时间
     */
    public static LocalDateTime parseDate(String dateString) {
        return parse(dateString, getDateFormat());
    }

    /**
     * 解析时间(年月日时分秒)
     *
     * @param dateTimeString 时间字符串
     * @return 时间
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        return parse(dateTimeString, getDateTimeFormat());
    }

    /**
     * 解析时间字符串
     *
     * @param str       时间字符串
     * @param formatter 时间格式
     * @return 时间
     */
    public static LocalDateTime parse(String str, DateTimeFormatter formatter) {
        return LocalDateTime.parse(str, formatter);
    }
}
