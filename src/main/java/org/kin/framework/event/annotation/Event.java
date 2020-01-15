package org.kin.framework.event.annotation;

import java.lang.annotation.*;

/**
 * 标识事件的注释
 *
 * @author huangjianqin
 * @date 2020-01-12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface Event {
}
