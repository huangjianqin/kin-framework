package org.kin.framework.counter;

import java.util.*;

/**
 * counter组
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class CounterGroup {
    private final String group;
    /** counters */
    private volatile Map<String, Counter> counters = Collections.emptyMap();

    public CounterGroup(String group) {
        this.group = group;
    }

    /**
     * 不存在, 则创建新的
     * 基于copy-on-write update
     *
     * @return 指定counter
     */
    public Counter counter(String uuid) {
        Counter counter = counters.get(uuid);
        if (Objects.isNull(counter)) {
            synchronized (this) {
                counter = counters.get(uuid);
                if (Objects.nonNull(counter)) {
                    return counter;
                }

                Map<String, Counter> counters = new HashMap<>(this.counters);
                counter = new Counter(uuid);
                counters.put(uuid, counter);
                this.counters = counters;
            }
        }
        return counter;
    }

    /**
     * 重置counter
     */
    public void reset() {
        counters.values().forEach(Counter::reset);
    }

    //getter
    public String getGroup() {
        return group;
    }

    public synchronized Collection<Counter> getCounters() {
        return counters.values();
    }
}
