package org.kin.framework.counter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * counter组
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class CounterGroup {
    private final String group;
    /** counter */
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public CounterGroup(String group) {
        this.group = group;
    }

    /**
     * @return 指定counter, 不存在, 则创建新的
     */
    public Counter counter(String uuid) {
        return counters.computeIfAbsent(uuid, k -> new Counter(uuid));
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

    public Map<String, Counter> getCounters() {
        return counters;
    }
}
