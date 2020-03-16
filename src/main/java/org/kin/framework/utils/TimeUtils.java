package org.kin.framework.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2018/2/2
 */
public class TimeUtils {
    private static Logger logger = LoggerFactory.getLogger(TimeUtils.class);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    static {
        DATE_FORMAT.setLenient(false);
        DATETIME_FORMAT.setLenient(false);
    }

    //-------------------------------------------------------get------------------------------------------------------

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
     *
     * @return DateFormat实例
     */
    public static DateFormat getDateFormat() {
        return DATE_FORMAT;
    }

    /**
     * 返回 代表'年月日时分秒' 的 DateFormat
     *
     * @return DateFormat实例
     */
    public static DateFormat getDatetimeFormat() {
        return DATETIME_FORMAT;
    }

    //-------------------------------------------------------format or parse------------------------------------------------------

    /**
     * 格式化时间
     *
     * @param date 时间
     * @return 时间字符串(年月日)
     */
    public static String formatDate(Date date) {
        return getDateFormat().format(date);
    }

    /**
     * 格式化时间
     *
     * @param date 时间
     * @return 时间字符串(年月日时分秒)
     */
    public static String formatDateTime(Date date) {
        return getDatetimeFormat().format(date);
    }

    /**
     * 解析时间(年月日)
     *
     * @param dateString 时间字符串
     * @return 时间
     */
    public static Date parseDate(String dateString) {
        return parse(dateString, getDateFormat());
    }

    /**
     * 解析时间(年月日时分秒)
     *
     * @param dateString 时间字符串
     * @return 时间
     */
    public static Date parseDateTime(String dateString) {
        return parse(dateString, getDatetimeFormat());
    }

    /**
     * 解析时间字符串
     *
     * @param dateString 时间字符串
     * @param dateFormat 时间格式
     * @return 时间
     */
    public static Date parse(String dateString, DateFormat dateFormat) {
        try {
            Date date = dateFormat.parse(dateString);
            return date;
        } catch (Exception e) {
            logger.error("parse date error, dateString = {}, dateFormat={}; errorMsg = ", dateString, dateFormat, e.getMessage());
            return null;
        }
    }

    //-------------------------------------------------------change data-------------------------------------------------------

    /**
     * 增加日数
     *
     * @param date 时间
     * @param days 日数
     * @return 时间
     */
    public static Date addDays(Date date, int days) {
        return add(date, Calendar.DAY_OF_MONTH, days);
    }

    /**
     * 增加年数
     *
     * @param date  时间
     * @param years 年数
     * @return 时间
     */
    public static Date addYears(Date date, int years) {
        return add(date, Calendar.YEAR, years);
    }

    /**
     * 增加月数
     *
     * @param date   时间
     * @param months 月数
     * @return 时间
     */
    public static Date addMonths(Date date, int months) {
        return add(date, Calendar.MONTH, months);
    }

    /**
     * 增加Calendar某个字段值
     *
     * @param date          时间
     * @param calendarField Calendar字段
     * @param value         值
     * @return 时间
     */
    private static Date add(Date date, int calendarField, int value) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, value);
        return c.getTime();
    }
}
