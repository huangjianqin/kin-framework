package org.kin.framework.beans;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.UnsafeUtil;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 基于{@link sun.misc.Unsafe}实现的bean copy
 *
 * @author huangjianqin
 * @date 2021/12/25
 */
@SuppressWarnings("rawtypes")
final class UnsafeBeanCopy extends PolymorphicCopy {
    static final UnsafeBeanCopy INSTANCE = new UnsafeBeanCopy();

    /** soft reference && 5 min ttl */
    private static final Cache<Integer, Copy> COPY_CACHE =
            CacheBuilder.newBuilder()
                    .softValues()
                    .expireAfterAccess(5, TimeUnit.MINUTES)
                    .build();

    private UnsafeBeanCopy() {
    }

    @Override
    public void copyProperties(Object source, Object target) {
        Copy copy = null;
        Class<?> sourceClass = source.getClass();
        Class<?> targetClass = target.getClass();
        try {
            copy = COPY_CACHE.get(cacheKey(sourceClass, targetClass), () -> genCopy(sourceClass, targetClass));
        } catch (ExecutionException e) {
            ExceptionUtils.throwExt(e);
        }
        copy.copyProperties(source, target);
    }

    /**
     * 生成{@link Copy}实例
     */
    private Copy genCopy(Class<?> sourceClass, Class<?> targetClass) {
        BeanInfoDetails sourceBid = BeanUtils.getBeanInfo(sourceClass);
        BeanInfoDetails targetBid = BeanUtils.getBeanInfo(targetClass);

        List<FieldAddressMapper> mappers = new ArrayList<>();
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

            long sourceAddress = UnsafeUtil.objectFieldOffset(readMethod.getDeclaringClass(), sourcePd.getName());
            long targetAddress = UnsafeUtil.objectFieldOffset(writeMethod.getDeclaringClass(), targetPd.getName());
            Class type = targetPd.getPropertyType();
            mappers.add(new FieldAddressMapper(type, sourceAddress, targetAddress));
        }

        return new FieldAddressCopy(mappers);
    }

    /**
     * bean source和copy target字段的内存地址映射
     */
    private static class FieldAddressMapper {
        private final Class type;
        /** source field内存地址 */
        private final long sourceAddress;
        /** target field内存地址 */
        private final long targetAddress;

        public FieldAddressMapper(Class type, long sourceAddress, long targetAddress) {
            this.type = type;
            this.sourceAddress = sourceAddress;
            this.targetAddress = targetAddress;
        }

        //getter
        public Class getType() {
            return type;
        }

        public long getSourceAddress() {
            return sourceAddress;
        }

        public long getTargetAddress() {
            return targetAddress;
        }
    }

    /**
     * 基于内存地址映射的bean field复制
     */
    private static class FieldAddressCopy implements Copy<Object, Object> {
        private final List<FieldAddressMapper> mappers;

        public FieldAddressCopy(List<FieldAddressMapper> mappers) {
            this.mappers = Collections.unmodifiableList(mappers);
        }

        @Override
        public void copyProperties(Object source, Object target) {
            for (FieldAddressMapper mapper : mappers) {
                Class type = mapper.getType();
                long sourceAddress = mapper.getSourceAddress();
                long targetAddress = mapper.getTargetAddress();

                //primitive处理
                if (Boolean.TYPE.equals(type)) {
                    UnsafeUtil.putBoolean(target, targetAddress, UnsafeUtil.getBoolean(source, sourceAddress));
                } else if (Byte.TYPE.equals(type)) {
                    UnsafeUtil.putByte(target, targetAddress, UnsafeUtil.getByte(source, sourceAddress));
                } else if (Character.TYPE.equals(type)) {
                    UnsafeUtil.putChar(target, targetAddress, (char) UnsafeUtil.getChar(source, sourceAddress));
                } else if (Short.TYPE.equals(type)) {
                    UnsafeUtil.putShort(target, targetAddress, (short) UnsafeUtil.getShort(source, sourceAddress));
                } else if (Integer.TYPE.equals(type)) {
                    UnsafeUtil.putInt(target, targetAddress, UnsafeUtil.getInt(source, sourceAddress));
                } else if (Long.TYPE.equals(type)) {
                    UnsafeUtil.putLong(target, targetAddress, UnsafeUtil.getLong(source, sourceAddress));
                } else if (Float.TYPE.equals(type)) {
                    UnsafeUtil.putFloat(target, targetAddress, UnsafeUtil.getFloat(source, sourceAddress));
                } else if (Double.TYPE.equals(type)) {
                    UnsafeUtil.putDouble(target, targetAddress, UnsafeUtil.getDouble(source, sourceAddress));
                } else {
                    Object value = UnsafeUtil.getObject(source, sourceAddress);
                    //这里之所以要使用UnsafeBeanCopy.INSTANCE, 是因为我们需要获取不同类型之间的Copy实例
                    UnsafeUtil.putObject(target, targetAddress, BeanUtils.DEEP ? UnsafeBeanCopy.INSTANCE.selfCopy(value) : value);
                }
            }
        }
    }
}
