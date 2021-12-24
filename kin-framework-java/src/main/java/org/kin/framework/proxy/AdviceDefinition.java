package org.kin.framework.proxy;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.CollectionUtils;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * advice定义
 *
 * @author huangjianqin
 * @date 2021/12/1
 */
public final class AdviceDefinition {
    /** 包含的package或全限定类名 */
    private final Set<String> includes = new HashSet<>();
    /** 忽略的package或全限定类名 */
    private final Set<String> excludes = new HashSet<>();
    /** 切点定义, key -> 切面class, value -> 切点annotation */
    private final Map<Class<?>, Class<? extends Annotation>> adviceClass2PointcutAnnotation = new HashMap<>();

    /**
     * 包含的package或全限定类名
     */
    public AdviceDefinition include(String include) {
        return includes(Collections.singleton(include));
    }

    /**
     * 包含的package或全限定类名
     */
    public AdviceDefinition includes(String... includes) {
        return includes(Arrays.asList(includes));
    }

    /**
     * 包含的package或全限定类名
     */
    public AdviceDefinition includes(Collection<String> includes) {
        this.includes.addAll(includes);
        return this;
    }

    /**
     * 忽略的package或全限定类名
     */
    public AdviceDefinition exclude(String exclude) {
        return excludes(Collections.singleton(exclude));
    }

    /**
     * 忽略的package或全限定类名
     */
    public AdviceDefinition excludes(String... excludes) {
        return excludes(Arrays.asList(excludes));
    }

    /**
     * 忽略的package或全限定类名
     */
    public AdviceDefinition excludes(Collection<String> excludes) {
        this.excludes.addAll(excludes);
        return this;
    }

    /**
     * 定义切面advice和切点pointcut annotation映射关系
     */
    public AdviceDefinition adviceAndPointcutAnnotation(Class<?> adviceClass, Class<? extends Annotation> pointcutAnnotation) {
        adviceClass2PointcutAnnotation.put(adviceClass, pointcutAnnotation);
        return this;
    }

    void check() {
        Preconditions.checkArgument(CollectionUtils.isNonEmpty(includes), "includes can not be empty");
        Preconditions.checkArgument(!adviceClass2PointcutAnnotation.isEmpty(), "advice class to pointcut annotation mapper can not be empty");
    }

    //getter
    public Set<String> getIncludes() {
        return includes;
    }

    public Set<String> getExcludes() {
        return excludes;
    }

    public Map<Class<?>, Class<? extends Annotation>> getAdviceClass2PointcutAnnotation() {
        return adviceClass2PointcutAnnotation;
    }
}
