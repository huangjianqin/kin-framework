package org.kin.framework.proxy;

import java.lang.annotation.*;

/**
 * 切点注解标识
 *
 * @author huangjianqin
 * @date 2021/12/2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@Inherited
public @interface Pointcut {
}
