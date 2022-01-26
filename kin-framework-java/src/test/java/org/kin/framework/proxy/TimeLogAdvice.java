package org.kin.framework.proxy;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2021/12/2
 */
public class TimeLogAdvice {
    @Advice.OnMethodEnter
    public static long enter(@Advice.AllArguments Object[] args, @Advice.Origin Method method) {
        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit(onThrowable = Exception.class)
    public static void exit(@Advice.Enter long startTime,
                            @Advice.Return(typing = Assigner.Typing.DYNAMIC) Object result,
                            @Advice.Origin Method method,
                            @Advice.Thrown Throwable throwable) {
        //本质上还是在PointcutInstance作用域范围内, 不能访问PointcutInstance作用域外的非public字段方法
        Logger logger = LoggerFactory.getLogger(TimeLogAdvice.class);
        if (Objects.nonNull(throwable)) {
            logger.error("方法('{}')执行异常, 耗时{}, ", method.getDeclaringClass().getCanonicalName().concat("#").concat(method.getName()), System.currentTimeMillis() - startTime, throwable);
        } else {
            logger.info("方法('{}')执行正常, 结果{}, 耗时{}", method.getDeclaringClass().getCanonicalName().concat("#").concat(method.getName()), result, System.currentTimeMillis() - startTime);
        }
    }
}
