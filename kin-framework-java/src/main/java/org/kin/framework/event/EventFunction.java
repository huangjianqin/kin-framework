package org.kin.framework.event;

import java.lang.annotation.*;

/**
 * 标识事件处理具体逻辑方法, 用于spring支持方法级注册事件及其处理器
 *
 * @author huangjianqin
 * @date 2019/3/1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
@Documented
public @interface EventFunction {
}
