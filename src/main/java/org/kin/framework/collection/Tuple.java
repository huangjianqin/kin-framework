package org.kin.framework.collection;

/**
 * @author huangjianqin
 * @date 2019-12-27
 */
public class Tuple<FIRST, SECOND> {
    private final FIRST first;
    private final SECOND second;

    public Tuple(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    //getter

    public FIRST first() {
        return first;
    }

    public SECOND second() {
        return second;
    }
}
