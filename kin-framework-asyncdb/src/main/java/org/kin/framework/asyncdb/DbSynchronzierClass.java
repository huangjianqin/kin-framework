package org.kin.framework.asyncdb;

import java.lang.annotation.*;

/**
 * @author huangjianqin
 * @date 2019/3/31
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface DbSynchronzierClass {
    /**
     * 持久化类 类型
     */
    Class<? extends DbSynchronzier<?, ?>> type();
}
