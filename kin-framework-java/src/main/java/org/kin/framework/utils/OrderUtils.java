package org.kin.framework.utils;

import org.kin.framework.common.Order;
import org.kin.framework.common.Ordered;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 针对{@link org.kin.framework.common.Order}和{@link org.kin.framework.common.Ordered}的工具类
 *
 * @author huangjianqin
 * @date 2021/3/15
 */
@SuppressWarnings("rawtypes")
public final class OrderUtils {
    private OrderUtils() {
    }

    /** order Comparator */
    private static final Comparator DEFAULT = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {
            return Integer.compare(getOrder(o1), getOrder(o2));
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    };

    /**
     * @return org.springframework.core.annotation.Order注解的value
     */
    @SuppressWarnings("unchecked")
    public static int getSpringOrder(Object o) {
        //寻找classpath里面有没有spring order注解, 如果有就优先使用spring order, 再解析自定义order注解
        Class<? extends Annotation> springOrderClass = null;
        try {
            springOrderClass = (Class<? extends Annotation>) Class.forName("org.springframework.core.annotation.Order");
        } catch (ClassNotFoundException e) {
            //do nothing
        }

        if (Objects.nonNull(springOrderClass)) {
            Annotation springOrderAnno = o.getClass().getAnnotation(springOrderClass);
            if (Objects.nonNull(springOrderAnno)) {
                try {
                    Method valueMethod = springOrderClass.getMethod("value");
                    if (!valueMethod.isAccessible()) {
                        valueMethod.setAccessible(true);
                    }
                    return (int) valueMethod.invoke(springOrderAnno);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    //do nothing
                }
            }
        }

        throw new IllegalStateException("can't get org.springframework.core.annotation.Order's order value");
    }

    /**
     * @see {@link OrderUtils#getOrder(Object, int)}
     */
    public static int getOrder(Object o) {
        return getOrder(o, Ordered.LOWEST_PRECEDENCE);
    }

    /**
     * 指定规则里面找到order value, 则返回, 否则返回default
     *
     * @param defaultOrder 默认order value
     * @return 该实例的order value
     */
    public static int getOrder(Object o, int defaultOrder) {
        if (o instanceof Ordered) {
            return ((Ordered) o).getOrder();
        } else {
            try {
                return getSpringOrder(o);
            } catch (Exception e) {
                //do nothing
            }

            Order orderAnno = o.getClass().getAnnotation(Order.class);
            if (Objects.nonNull(orderAnno)) {
                return orderAnno.value();
            }
        }

        return defaultOrder;
    }

    /**
     * item带{@link org.kin.framework.common.Order}注解或者实现了{@link org.kin.framework.common.Ordered}接口的集合类排序
     */
    @SuppressWarnings("unchecked")
    public static void sort(List<?> list) {
        list.sort(DEFAULT);
    }
}
