package org.kin.framework.common;

import java.lang.annotation.*;

/**
 * 带该注解的类(比如在集合里)必须保证有序
 *
 * @author huangjianqin
 * @date 2021/3/15
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface Order {
    /**
     * The order value.
     * Default is {@link Ordered#LOWEST_PRECEDENCE}.
     *
     * @see org.springframework.core.Ordered#getOrder()
     */
    int value() default Ordered.LOWEST_PRECEDENCE;
}
