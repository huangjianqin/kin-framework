package org.kin.framework.actor.domain;

/**
 * @author huangjianqin
 * @date 2018/6/5
 * <p>
 * 终止Actor的message类
 */
public final class PoisonPill {
    private static final PoisonPill INSTANCE = new PoisonPill();

    private PoisonPill() {

    }

    public static PoisonPill instance() {
        return INSTANCE;
    }
}
