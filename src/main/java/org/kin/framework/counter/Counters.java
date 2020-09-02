package org.kin.framework.counter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * counter 对外api
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class Counters {
    /** counter */
    static final Map<String, CounterGroup> counterGroups = new ConcurrentHashMap<>();

    /**
     * @return 指定counter group, 不存在, 则创建新的
     */
    public static CounterGroup counterGroup(String group) {
        CounterGroup counterGroup = new CounterGroup(group);
        CounterGroup old = counterGroups.putIfAbsent(group, counterGroup);
        if (Objects.isNull(old)) {
            return counterGroup;
        } else {
            return old;
        }
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
}
