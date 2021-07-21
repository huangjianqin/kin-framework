package org.kin.framework.statemachine;

/**
 * 一对一状态转换逻辑处理
 *
 * @author huangjianqin
 * @date 2017/8/9
 */
@FunctionalInterface
public interface SingleArcTransition<OPERAND, EVENT> {
    /**
     * @param operand 操作
     * @param event   事件
     */
    void transition(OPERAND operand, EVENT event);
}
