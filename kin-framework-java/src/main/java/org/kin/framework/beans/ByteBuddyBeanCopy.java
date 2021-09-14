package org.kin.framework.beans;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 基于ByteBuddy字节码增强后的bean copy properties工具类
 *
 * @author huangjianqin
 * @date 2021/9/8
 */
public class ByteBuddyBeanCopy extends BaseCopy {
    public static final ByteBuddyBeanCopy INSTANCE = new ByteBuddyBeanCopy();

    /** soft reference && 30 min ttl */
    private static final Cache<Integer, Copy> COPY_CACHE =
            CacheBuilder.newBuilder()
                    .softValues()
                    .expireAfterAccess(30, TimeUnit.MINUTES)
                    .build();
    /** {@link Copy#copyProperties(Object, Object)}source参数下标 */
    private static final int SOURCE_ARG_INDEX = 0;
    /** {@link Copy#copyProperties(Object, Object)}target参数下标 */
    private static final int TARGET_ARG_INDEX = 1;
    /** {@link ByteBuddyBeanCopy#selfCopyBridge(Object)}方法, 用于深复制 */
    private static final Method SELF_COPY_BRIDGE_METHOD;
    /** {@link Copy#copyProperties(Object, Object)}方法 */
    private static final Method COPY_PROPERTIES_METHOD;

    static {
        try {
            SELF_COPY_BRIDGE_METHOD = ByteBuddyBeanCopy.class.getMethod("selfCopyBridge", Object.class);
            COPY_PROPERTIES_METHOD = Copy.class.getMethod("copyProperties", Object.class, Object.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 桥接方法, 方便{@link MethodCall}直接桥接, 然后调用真正逻辑
     */
    public static Object selfCopyBridge(Object source) {
        return INSTANCE.selfCopy(source);
    }

    private ByteBuddyBeanCopy() {
    }

    @Override
    public void copyProperties(Object source, Object target) {
        Copy copy = null;
        Class<?> sourceC = source.getClass();
        Class<?> targetC = target.getClass();
        try {
            copy = COPY_CACHE.get(cacheKey(sourceC, targetC), () -> genCopy(sourceC, targetC));
        } catch (ExecutionException e) {
            ExceptionUtils.throwExt(e);
        }
        copy.copyProperties(source, target);
    }

    /**
     * 获取缓存唯一key
     */
    private int cacheKey(Class<?> sourceC, Class<?> targetC) {
        //使用hashcode, 节省内存
        return sourceC.getCanonicalName().concat("=>").concat(targetC.getCanonicalName()).hashCode();
    }

    /**
     * 获取{@link Copy}实现类类名
     */
    private String copyClassName(Class<?> sourceC, Class<?> targetC) {
        return sourceC.getCanonicalName().concat("$").concat(targetC.getCanonicalName()).concat("$").concat(Copy.class.getSimpleName());
    }

    /**
     * 基于ByteBuddy生成{@link Copy}实现类
     */
    private Copy genCopy(Class<?> sourceC, Class<?> targetC) {
        BeanInfoDetails sourceBid = BeanInfoCache.INSTANCE.get(sourceC);
        BeanInfoDetails targetBid = BeanInfoCache.INSTANCE.get(targetC);

        Implementation.Composable definition = null;
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

            if (Objects.isNull(definition)) {
                definition = setter(writeMethod, getter(readMethod));
            } else {
                definition = definition.andThen(setter(writeMethod, getter(readMethod)));
            }
        }

        Class<?> copyImplClass = new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(Object.class)
                .implement(Copy.class)
                .name(copyClassName(sourceC, targetC))
                .method(ElementMatchers.anyOf(COPY_PROPERTIES_METHOD))
                .intercept(definition)
                .make()
                .load(ByteBuddyBeanCopy.class.getClassLoader())
                .getLoaded();

        return (Copy) ClassUtils.instance(copyImplClass);
    }

    /**
     * 生成目标setter方法逻辑
     */
    private Implementation.Composable setter(Method writeMethod, MethodCall readImpl) {
        if (BeanUtils.DEEP) {
            //深复制
            MethodCall deepCopyImpl = MethodCall.invoke(SELF_COPY_BRIDGE_METHOD).withMethodCall(readImpl);
            return MethodCall.invoke(writeMethod).onArgument(TARGET_ARG_INDEX).withMethodCall(deepCopyImpl).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
        } else {
            //浅复制
            return MethodCall.invoke(writeMethod).onArgument(TARGET_ARG_INDEX).withMethodCall(readImpl).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
        }
    }

    /**
     * 生成源getter方法逻辑
     */
    private MethodCall getter(Method readMethod) {
        return (MethodCall) MethodCall.invoke(readMethod).onArgument(SOURCE_ARG_INDEX).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC);
    }
}
