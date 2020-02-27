package org.kin.framework.statemachine;

/**
 * @author 健勤
 * @date 2017/8/9
 * 状态机接口
 */
public interface StateMachine<STATE extends Enum<STATE>, EVENTTYPE extends Enum<EVENTTYPE>, EVENT> {
    /**
     * @return 当前状态
     */
    STATE getCurrentState();

    /**
     * @param eventType 事件类型
     * @param event     事件
     * @return 事件触发后的状态
     */
    STATE doTransition(EVENTTYPE eventType, EVENT event);
}
