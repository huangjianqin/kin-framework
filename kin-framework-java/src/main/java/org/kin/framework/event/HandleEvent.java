package org.kin.framework.event;

import java.lang.annotation.*;

/**
 * 用于标识带有{@link EventFunction}注解方法的class
 *
 * @author huangjianqin
 * @date 2021/3/13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface HandleEvent {
}
