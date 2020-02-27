package org.kin.framework.statemachine;

/**
 * @author 健勤
 * @date 2017/8/9
 * 一对一状态转换逻辑处理
 */
@FunctionalInterface
public interface SingleArcTransition<OPERAND, EVENT> {
    /**
     * @param operand 操作
     * @param event   事件
     */
    void transition(OPERAND operand, EVENT event);
}
