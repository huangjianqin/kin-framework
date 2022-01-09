package org.kin.framework.counter;

import java.util.*;

/**
 * counter 对外api
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class Counters {
    /** counter groups */
    private static volatile Map<String, CounterGroup> counterGroups = Collections.emptyMap();

    /**
     * 不存在, 则创建新的
     * 基于copy-on-write update
     *
     * @return 指定counter group
     */
    public static CounterGroup counterGroup(String group) {
        CounterGroup counterGroup = Counters.counterGroups.get(group);
        if (Objects.isNull(counterGroup)) {
            synchronized (CounterGroup.class) {
                counterGroup = Counters.counterGroups.get(group);
                if (Objects.nonNull(counterGroup)) {
                    return counterGroup;
                }

                Map<String, CounterGroup> counterGroups = new HashMap<>(Counters.counterGroups);
                counterGroup = new CounterGroup(group);
                counterGroups.put(group, counterGroup);
                Counters.counterGroups = counterGroups;
            }
        }

        return counterGroup;
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
