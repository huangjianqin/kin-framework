package org.kin.framework.concurrent;

import org.kin.framework.utils.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author huangjianqin
 * @date 2020-03-18
 * <p>
 * 时间轮
 */
public class TimeRing<D> implements KeeperAction {
    private static final Logger log = LoggerFactory.getLogger(TimeRing.class);
    /** 时间轮每个slot对应的数据 */
    private Map<Integer, List<D>> ringData;
    /** 时间轮slot数量 */
    private int slot;
    /** 时间轮刻度前进时间, 毫秒 */
    private long unit;
    /** 每次处理slot数量 */
    private int slotPerRound;
    /** 时间轮slot数据处理 */
    private SlotDataHandler<D> slotDataHandler;
    //------------------------------------------------------------------------------------------------------------
    private Keeper.KeeperStopper stopper;

    public TimeRing(int slot, long unit, SlotDataHandler<D> slotDataHandler) {
        this(slot, unit, 2, slotDataHandler);
    }

    public TimeRing(int slot, long unit, int slotPerRound, SlotDataHandler<D> slotDataHandler) {
        this.slot = slot;
        this.unit = unit;
        this.slotPerRound = slotPerRound;
        this.slotDataHandler = slotDataHandler;
    }

    public static <D> TimeRing<D> second(SlotDataHandler<D> slotDataHandler) {
        return second(1, slotDataHandler);
    }

    public static <D> TimeRing<D> second(int slotPerRound, SlotDataHandler<D> slotDataHandler) {
        return new TimeRing<>(60, 1000, slotPerRound, slotDataHandler);
    }

    public void start() {
        if (Objects.nonNull(stopper)) {
            stopper.stop();
        }
        ringData = new ConcurrentHashMap<>(slot * 3 / 2);
        stopper = Keeper.keep(this);
    }

    public void push(long time, D data) {
        int ringSlot = (int) ((time / unit) % slot);
        List<D> ringItemData = ringData.computeIfAbsent(ringSlot, k -> new ArrayList<>());
        ringItemData.add(data);
    }

    public void stop() {
        stopper.stop();
        stopper = null;
    }

    @Override
    public void preAction() {
        //do nothing
    }

    @Override
    public void action() {
        //align slot
        try {
            Thread.sleep(unit * slotPerRound - System.currentTimeMillis() % unit);
        } catch (InterruptedException e) {

        }

        try {
            // slot data
            List<D> ringSlotData = new ArrayList<>();
            int nowSlot = (int) (System.currentTimeMillis() / unit);
            for (int i = 0; i < slotPerRound; i++) {
                List<D> tmp = ringData.remove((nowSlot - i) % slot);
                if (CollectionUtils.isNonEmpty(tmp)) {
                    ringSlotData.addAll(tmp);
                }
            }

            // ring trigger
            if (CollectionUtils.isNonEmpty(ringSlotData)) {
                // do trigger
                for (D slotData : ringSlotData) {
                    // do trigger
                    slotDataHandler.handle(slotData);
                }
                // clear
                ringSlotData.clear();
            }
        } catch (Exception e) {
            log.error("时钟调度遇到异常 >>>> ", e);
        }
    }

    @Override
    public void postAction() {
        //do nothing
    }

    @FunctionalInterface
    public interface SlotDataHandler<D> {
        /**
         * 处理时间轮slot数据
         * 最好异步处理任务
         *
         * @param slotData slot数据
         */
        void handle(D slotData);
    }
}
