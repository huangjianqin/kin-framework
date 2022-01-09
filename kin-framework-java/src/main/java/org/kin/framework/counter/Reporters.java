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
        for (CounterGroup group : Counters.getAllGroup()) {
            out.append(String.format("----------------------group: %s----------------------", group.getGroup())).append(System.lineSeparator());
            for (Counter counter : group.getCounters()) {
                out.append(counter.report()).append(System.lineSeparator());
            }
        }
        out.append("----------------------------------------------------------------------------");
        return out.toString();
    }
}
