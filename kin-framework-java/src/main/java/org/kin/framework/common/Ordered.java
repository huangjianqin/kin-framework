package org.kin.framework.common;

/**
 * 该接口的实现类必须保证有序(比如在集合中)
 * {@link Ordered#getOrder()}返回值越小, 优先级越高
 * <p>
 * 优先级比{@link Order}高
 *
 * @author huangjianqin
 * @date 2021/3/15
 */
public interface Ordered {
    /**
     * 最高优先级
     */
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * 最低优先级
     */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * @return the order value
     */
    int getOrder();
}
