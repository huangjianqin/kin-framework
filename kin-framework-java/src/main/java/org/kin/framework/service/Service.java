package org.kin.framework.service;

import org.kin.framework.Closeable;

/**
 * 服务接口
 *
 * @author huangjianqin
 * @date 2017/8/8
 */
public interface Service extends Closeable {
    /**
     * 服务初始化
     */
    void init();

    /**
     * 服务启动
     */
    void start();

    /**
     * 服务关闭
     */
    void stop();

    /**
     * 等待服务结束
     *
     * @param mills 等待时间, 毫秒
     * @return 服务结束结果
     */
    boolean waitForServiceToStop(long mills);

    /**
     * 注册监听服务状态变化listener
     *
     * @param listener listener实现类
     */
    void registerServiceListener(ServiceStateChangeListener listener);

    /**
     * 取消注册监听服务状态变化listener
     *
     * @param listener listener实现类
     */
    void unregisterServiceListener(ServiceStateChangeListener listener);

    /**
     * @param that 指定状态
     * @return 当前状态是否是指定状态
     */
    boolean isInState(State that);

    /**
     * @return 服务名
     */
    String getName();

    /**
     * @return 服务当前状态
     */
    State getCurrentState();

    /**
     * @return 服务启动时间
     */
    long getStartTime();

    /**
     * 服务状态的枚举类
     */
    enum State {
        /**
         * 服务未初始化状态
         */
        NOTINITED(0, "NOTINITED"),
        /**
         * 服务已初始化状态
         */
        INITED(1, "INITED"),
        /**
         * 服务已启动
         */
        STARTED(2, "STARTED"),
        /**
         * 服务已停止
         */
        STOPPED(3, " STOPPED");

        private final int stateId;
        private final String stateName;

        State(int stateId, String stateName) {
            this.stateId = stateId;
            this.stateName = stateName;
        }

        static State getById(int stateId) {
            if (stateId == NOTINITED.getStateId()) {
                return NOTINITED;
            } else if (stateId == INITED.getStateId()) {
                return INITED;
            } else if (stateId == STARTED.getStateId()) {
                return STARTED;
            } else if (stateId == STOPPED.getStateId()) {
                return STOPPED;
            } else {
                throw new IllegalStateException("unknown state id");
            }
        }

        static State getByName(String stateName) {
            if (stateName.toUpperCase().equals(NOTINITED.getStateName())) {
                return NOTINITED;
            } else if (stateName.toUpperCase().equals(INITED.getStateName())) {
                return INITED;
            } else if (stateName.toUpperCase().equals(STARTED.getStateName())) {
                return STARTED;
            } else if (stateName.toUpperCase().equals(STOPPED.getStateName())) {
                return STOPPED;
            } else {
                throw new IllegalStateException("unknown state name");
            }
        }

        int getStateId() {
            return stateId;
        }

        String getStateName() {
            return stateName;
        }

        @Override
        public String toString() {
            return "Service.State{" +
                    "stateName='" + stateName + '\'' +
                    '}';
        }
    }
}
