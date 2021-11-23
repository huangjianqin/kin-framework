package org.kin.framework.beans;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SPI;
import org.springframework.lang.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;

/**
 * 使用者自定义加载{@link BeanInfo}逻辑
 * 通过{@link ExtensionLoader}加载
 *
 * @author huangjianqin
 * @date 2021/9/8
 */
@SPI(alias = "beanInfoFactory")
public interface BeanInfoFactory {
    /**
     * 如果支持的话, 则返回响应{@link BeanInfo}
     */
    @Nullable
    BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException;
}
