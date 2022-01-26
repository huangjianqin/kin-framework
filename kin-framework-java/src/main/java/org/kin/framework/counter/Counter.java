package org.kin.framework.counter;

import java.util.concurrent.atomic.LongAdder;

/**
 * 计数器
 * 内部统计需保证一致性
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
public class Counter implements Reporter {
    /** uuid */
    private final String uuid;
    /** count */
    private final LongAdder count;

    Counter(String uuid) {
        this.uuid = uuid;
        this.count = new LongAdder();
    }

    /**
     * 递增
     *
     * @return 当前计数值
     */
    public long increment() {
        return increment(1L);
    }

    /**
     * 增加{@code value}
     *
     * @return 当前计数值
     */
    public long increment(long value) {
        count.add(value);
        return count();
    }

    /**
     * 重置计数器
     *
     * @return 当前计数值
     */
    public long reset() {
        return count.sumThenReset();
    }

    /**
     * @return 当前计数值
     */
    public long count() {
        return count.sum();
    }

    @Override
    public String report() {
        //上报后会重置
        long count = this.count.sumThenReset();
        return uuid.concat("-").concat(String.valueOf(count));
    }
}
