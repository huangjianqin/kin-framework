package org.kin.framework.asyncdb;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 支持异步db
 *
 * @author huangjianqin
 * @date 2020/12/27
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@Import(AsyncDbConfiguration.class)
public @interface EnableAsyncDb {
}
