package org.kin.framework.counter;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * counter组
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class CounterGroup {
    final String group;
    /** counter */
    Map<String, Counter> counters = new ConcurrentHashMap<>();

    public CounterGroup(String group) {
        this.group = group;
    }

    /**
     * @return 指定counter, 不存在, 则创建新的
     */
    public Counter counter(String uuid) {
        Counter counter = new Counter(uuid);
        Counter old = counters.putIfAbsent(uuid, counter);
        if (Objects.isNull(old)) {
            return counter;
        } else {
            return old;
        }
    }
}
