package org.kin.framework.asyncdb;

/**
 * @author huangjianqin
 * @date 2019/4/3
 */
public class DefaultAsyncDbStrategy implements AsyncDbStrategy {
    @Override
    public int getOprNum() {
        return 10;
    }

    @Override
    public int getTryTimes() {
        return 2;
    }

    @Override
    public int getDuration(int size) {
        if (size > 50) {
            return 200;
        }
        return 1000;
    }
}
