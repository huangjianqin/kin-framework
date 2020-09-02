package org.kin.framework.counter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 计数器
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class Counter implements Reporter {
    /** uuid */
    private String uuid;
    /** count */
    private AtomicLong count;

    Counter(String uuid) {
        this.uuid = uuid;
        this.count = new AtomicLong(0);
    }

    /**
     * 增量
     *
     * @return 当前计数值
     */
    public long increment() {
        return increment(0L);
    }

    /**
     * 增量
     *
     * @return 当前计数值
     */
    public long increment(long amount) {
        return count.addAndGet(amount);
    }

    /**
     * 重置计数器
     *
     * @return 当前计数值
     */
    public long reset() {
        return count.getAndSet(0);
    }

    /**
     * @return 当前计数值
     */
    public long count() {
        return count.get();
    }

    @Override
    public String report() {
        return uuid.concat("-").concat(String.valueOf(count()));
    }
}
