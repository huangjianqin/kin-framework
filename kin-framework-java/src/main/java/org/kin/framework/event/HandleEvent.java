package org.kin.framework.event;

import java.lang.annotation.*;

/**
 * 处理事件的具体逻辑实现
 * 注解事件处理器 or 方法
 *
 * @author huangjianqin
 * @date 2019/3/1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface HandleEvent {
}
