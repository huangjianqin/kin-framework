package org.kin.framework.counter;

/**
 * @author huangjianqin
 * @date 2020/9/2
 */
public class Reporters {
    /**
     * 生成指定的report
     */
    public static String report() {
        StringBuilder out = new StringBuilder();
        out.append("-------------------------------counter report-------------------------------");
        out.append(System.lineSeparator());
        for (CounterGroup counterGroup : Counters.counterGroups.values()) {
            out.append("<<<").append(counterGroup.group).append(">>>").append(System.lineSeparator());
            for (Counter value : counterGroup.counters.values()) {
                out.append(value.report()).append(System.lineSeparator());
            }
            out.append("<<<<<<");
            out.append(System.lineSeparator());
        }
        out.append("----------------------------------------------------------------------------");
        return out.toString();
    }
}
