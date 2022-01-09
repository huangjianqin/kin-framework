package org.kin.framework.counter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * counter 对外api
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class Counters {
    /** counter groups */
    private static volatile Map<String, CounterGroup> counterGroups = new ConcurrentHashMap<>();

    /**
     * 不存在, 则创建新的
     *
     * @return 指定counter group
     */
    public static CounterGroup counterGroup(String group) {
        return counterGroups.computeIfAbsent(group, k -> new CounterGroup(group));
    }

    /**
     * 计数器增量
     */
    public static void increment(String group, String counter) {
        increment(group, counter, 1L);
    }

    /**
     * 计数器增量
     */
    public static void increment(String group, String counter, long amount) {
        counterGroup(group).counter(counter).increment(amount);
    }

    /**
     * 重置counter
     */
    public static synchronized void reset() {
        counterGroups.values().forEach(CounterGroup::reset);
    }

    /**
     * 获取所有counter group
     */
    public static synchronized Collection<CounterGroup> getAllGroup() {
        return counterGroups.values();
    }
}
