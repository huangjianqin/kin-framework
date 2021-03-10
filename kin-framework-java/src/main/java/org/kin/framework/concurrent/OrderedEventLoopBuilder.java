package org.kin.framework.concurrent;

/**
 * @author huangjianqin
 * @date 2021/1/26
 */
@FunctionalInterface
public interface OrderedEventLoopBuilder<P extends OrderedEventLoop<P>> {
    /** 构建自定义{@link OrderedEventLoop} */
    P build(EventLoopGroup<P> eventLoopGroup, ExecutionContext ec);
}
