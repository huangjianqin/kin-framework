package org.kin.framework.event;

/**
 * 事件合并类型
 *
 * @author huangjianqin
 * @date 2021/3/13
 */
public enum MergeType {
    /**
     * 窗口, 一定时间内, 相同时间合并的List<Event>并分发
     */
    WINDOW,
    /**
     * 抖动, 一定时间内, 如果没有任何EVENT触发, 则将之前收集到的所有该EVENT合并的List<Event>并分发
     * 适用于场景:
     * 1. 频繁触发指定事件, 但仅仅需要最新的事件状态
     */
    DEBOUNCE,
    ;
}
