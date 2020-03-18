//package org.kin.framework.actor;
//
///**
// * @author huangjianqin
// * @date 2018/6/5
// * <p>
// * 处理预定义方法匹配接口
// */
//public interface Receive {
//    @FunctionalInterface
//    interface Func<AA extends AbstractActor<AA>, T> {
//        /**
//         * @param applier 事件处理actor
//         * @param message 事件
//         * @throws Exception 事件处理抛去异常
//         */
//        void apply(AA applier, T message) throws Exception;
//    }
//
//    /**
//     * @param applier 事件处理actor
//     * @param message 事件
//     * @param <AA>    actor实现类
//     * @param <T>     事件实现类
//     */
//    <AA extends AbstractActor<AA>, T> void receive(AA applier, T message);
//}
