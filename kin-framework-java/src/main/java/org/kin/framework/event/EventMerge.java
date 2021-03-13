package org.kin.framework.event;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 事件合并参数,
 * 支持合并的事件, 注册时, event class也是事件本身, 也就是不能同时注册event和List<event>的事件处理器
 *
 * @author huangjianqin
 * @date 2021/3/13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EventMerge {
    /** 事件合并类型 */
    MergeType type() default MergeType.WINDOW;

    /** 窗口时间 */
    long window();

    /** 时间单位 */
    TimeUnit unit() default TimeUnit.MILLISECONDS;
}
