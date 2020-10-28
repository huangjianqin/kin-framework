package org.kin.framework.counter;

/**
 * 生成report str
 *
 * @author huangjianqin
 * @date 2020/9/2
 */
@FunctionalInterface
public interface Reporter {
    /**
     * @return report str
     */
    String report();
}
