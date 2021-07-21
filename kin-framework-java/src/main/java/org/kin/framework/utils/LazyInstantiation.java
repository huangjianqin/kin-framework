package org.kin.framework.utils;

import java.util.Objects;

/**
 * 延迟实例化(无参构造方法)
 * 线程安全
 *
 * @author huangjianqin
 * @date 2021/1/23
 */
public abstract class LazyInstantiation<C> {
    /** target class */
    protected final Class<? extends C> targetClass;
    /** 实例 */
    protected volatile C instance;

    public LazyInstantiation(Class<? extends C> targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * 实例初始化
     */
    protected void init(C instance) {

    }

    public C instance() {
        if (Objects.isNull(instance)) {
            synchronized (this) {
                if (Objects.isNull(instance)) {
                    C newInstance = ClassUtils.instance(targetClass);
                    init(newInstance);
                    //初始化完才赋值
                    this.instance = newInstance;
                }
            }
        }
        return instance;
    }

    //getter
    public Class<? extends C> getTargetClass() {
        return targetClass;
    }
}
