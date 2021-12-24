package org.kin.framework.proxy;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Objects;

import static net.bytebuddy.asm.Advice.to;
import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * @author huangjianqin
 * @date 2021/12/1
 */
public final class ByteBuddys {
    private static final Logger log = LoggerFactory.getLogger(ByteBuddys.class);

    private ByteBuddys() {
    }

    /**
     * 基于byte-buddy的aop实现, 但必须得在实例创建install
     * 如果要与spring兼容, 需实现ApplicationListener<ApplicationEnvironmentPreparedEvent>, 在spring.factories且调用用{@link #installAdvices(AdviceDefinition)} ()}
     */
    public static synchronized void installAdvices(AdviceDefinition definition) {
        definition.check();

        ElementMatcher.Junction<NamedElement> includePointcuts = null;
        for (String include : definition.getIncludes()) {
            if (Objects.isNull(includePointcuts)) {
                includePointcuts = nameStartsWith(include);
            } else {
                includePointcuts = includePointcuts.or(nameStartsWith(include));
            }
        }

        ElementMatcher.Junction<NamedElement> excludePointcuts = none();
        for (String exclude : definition.getExcludes()) {
            if (Objects.isNull(excludePointcuts)) {
                excludePointcuts = nameStartsWith(exclude);
            } else {
                excludePointcuts = excludePointcuts.or(nameStartsWith(exclude));
            }
        }

        ByteBuddyAgent.install();
        new AgentBuilder.Default()
                .with(AgentBuilder.PoolStrategy.Default.EXTENDED)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                // 忽略指定包
                .ignore(nameStartsWith("org.aspectj.")
                        .or(nameStartsWith("org.groovy."))
                        .or(nameStartsWith("com.sun."))
                        .or(nameStartsWith("sun."))
                        .or(nameStartsWith("jdk."))
                        .or(nameStartsWith("java."))
                        .or(nameStartsWith("org.springframework.asm."))
                        .or(nameStartsWith("com.p6spy."))
                        .or(nameStartsWith("net.bytebuddy."))
                        .or(excludePointcuts)
                )
                // 匹配范围
                .type(includePointcuts)
                .transform((builder, typeDescription, classLoader, module) -> {
                    DynamicType.Builder<?> ret = builder;
                    for (Map.Entry<Class<?>, Class<? extends Annotation>> entry : definition.getAdviceClass2PointcutAnnotation().entrySet()) {
                        Class<?> adviceClass = entry.getKey();
                        Class<? extends Annotation> pointcutAnnotation = entry.getValue();
                        ret = ret.visit(to(adviceClass).on(isAnnotatedWith(pointcutAnnotation)));
                    }
                    return ret;
                })
                .with(new AgentBuilder.Listener() {
                    @Override
                    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                        log.debug("byte buddy advices discovery type {}", typeName);
                    }

                    @Override
                    public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
                        log.debug("byte buddy advices installing advices on {}", typeDescription.getCanonicalName());
                    }

                    @Override
                    public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded) {
                        log.debug("byte buddy advices ignore type {}", typeDescription.getCanonicalName());
                    }

                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        log.error("byte buddy advices encounter error, when install advice on {}", typeName, throwable);
                    }

                    @Override
                    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
                        log.info("byte buddy advices finish install advices on {}", typeName);
                    }
                })
                .installOnByteBuddyAgent();
        log.info("byte buddy advices is ready");
    }
}
