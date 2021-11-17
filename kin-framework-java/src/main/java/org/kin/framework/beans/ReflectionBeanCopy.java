package org.kin.framework.beans;

import org.kin.framework.utils.ClassUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * 基于反射的bean copy properties工具类
 *
 * @author huangjianqin
 * @date 2021/9/8
 */
class ReflectionBeanCopy extends BaseCopy {
    static final ReflectionBeanCopy INSTANCE = new ReflectionBeanCopy();

    private ReflectionBeanCopy() {
    }

    @Override
    public void copyProperties(Object source, Object target) {
        BeanInfoDetails sourceBid = BeanInfoCache.INSTANCE.get(source.getClass());
        BeanInfoDetails targetBid = BeanInfoCache.INSTANCE.get(target.getClass());

        for (PropertyDescriptor targetPd : targetBid.getPropertyDescriptors()) {
            Method writeMethod = targetPd.getWriteMethod();
            if (Objects.isNull(writeMethod)) {
                //setter不存在
                continue;
            }

            PropertyDescriptor sourcePd = sourceBid.getPdByName(targetPd.getName());
            if (Objects.isNull(sourcePd)) {
                //没有对应名字的field
                continue;
            }

            Method readMethod = sourcePd.getReadMethod();
            if (Objects.isNull(readMethod)) {
                //没有getter
                continue;
            }

            if (!ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                continue;
            }

            //copy reference
            try {
                if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                    readMethod.setAccessible(true);
                }
                Object value = readMethod.invoke(source);
                if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                    writeMethod.setAccessible(true);
                }
                writeMethod.invoke(target, BeanUtils.DEEP ? selfCopy(value) : value);
            } catch (Throwable ex) {
                throw new IllegalStateException(
                        "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
            }
        }
    }
}