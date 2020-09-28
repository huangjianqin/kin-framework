package org.kin.framework.utils;

import java.lang.annotation.*;

/**
 * 标识该接口支持SPI机制扩展
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SPI {
    /**
     * 配置文件的key
     */
    String value() default "";
}