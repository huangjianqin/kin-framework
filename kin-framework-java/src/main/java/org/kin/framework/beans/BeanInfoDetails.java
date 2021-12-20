package org.kin.framework.beans;

import com.google.common.collect.ImmutableMap;

import java.awt.*;
import java.beans.*;
import java.util.Map;

/**
 * BeanInfo包装类
 *
 * @author huangjianqin
 * @date 2021/9/8
 */
public final class BeanInfoDetails implements BeanInfo {
    /** 真正的{@link BeanInfo} */
    private final BeanInfo beanInfo;
    /** key -> field name, value -> {@link PropertyDescriptor}实例 */
    private final Map<String, PropertyDescriptor> name2Pd;

    BeanInfoDetails(BeanInfo beanInfo) {
        this.beanInfo = beanInfo;
        ImmutableMap.Builder<String, PropertyDescriptor> name2PdBuilder = ImmutableMap.builder();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            name2PdBuilder.put(pd.getName(), pd);
        }
        this.name2Pd = name2PdBuilder.build();
    }

    /**
     * 根据field name获取 {@link PropertyDescriptor}实例
     */
    public PropertyDescriptor getPdByName(String name) {
        return name2Pd.get(name);
    }

    //-------------------------------------wrapper-------------------------------------
    @Override
    public BeanDescriptor getBeanDescriptor() {
        return beanInfo.getBeanDescriptor();
    }

    @Override
    public EventSetDescriptor[] getEventSetDescriptors() {
        return beanInfo.getEventSetDescriptors();
    }

    @Override
    public int getDefaultEventIndex() {
        return beanInfo.getDefaultEventIndex();
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return beanInfo.getPropertyDescriptors();
    }

    @Override
    public int getDefaultPropertyIndex() {
        return beanInfo.getDefaultPropertyIndex();
    }

    @Override
    public MethodDescriptor[] getMethodDescriptors() {
        return beanInfo.getMethodDescriptors();
    }

    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        return beanInfo.getAdditionalBeanInfo();
    }

    @Override
    public Image getIcon(int iconKind) {
        return beanInfo.getIcon(iconKind);
    }

}
