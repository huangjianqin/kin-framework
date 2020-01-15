package org.kin.framework.event.annotation;

import java.lang.annotation.*;

/**
 * 处理事件的具体逻辑实现
 *
 * Created by huangjianqin on 2019/3/1.
 * <p>
 * 注解事件处理器 or 方法
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface HandleEvent {
}
