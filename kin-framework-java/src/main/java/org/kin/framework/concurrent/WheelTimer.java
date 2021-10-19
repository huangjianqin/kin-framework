package org.kin.framework.concurrent;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时间轮
 * 类似于调度, slot数量设置越多, 精度越大
 * 设定数据处理逻辑{@link SlotDataHandler}, 然后push未来某时刻需要处理的数据, 当轮转到该数据时, 进行数据处理
 *
 * @author huangjianqin
 * @date 2020-03-18
 */
public final class WheelTimer<D> implements KeeperAction {
    /** 时间轮每个slot对应的数据 */
    private ConcurrentHashMap<Integer, List<D>> ringData;
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

    public WheelTimer(int slot, long unit, SlotDataHandler<D> slotDataHandler) {
        this(slot, unit, 2, slotDataHandler);
    }

    public WheelTimer(int slot, long unit, int slotPerRound, SlotDataHandler<D> slotDataHandler) {
        this.slot = slot;
        this.unit = unit;
        this.slotPerRound = slotPerRound;
        this.slotDataHandler = slotDataHandler;
    }

    public static <D> WheelTimer<D> second(SlotDataHandler<D> slotDataHandler) {
        return second(1, slotDataHandler);
    }

    public static <D> WheelTimer<D> second(int slotPerRound, SlotDataHandler<D> slotDataHandler) {
        return new WheelTimer<>(60, 1000, slotPerRound, slotDataHandler);
    }

    /**
     * 开始轮转
     */
    public void start() {
        if (Objects.nonNull(stopper)) {
            stopper.stop();
        }
        ringData = new ConcurrentHashMap<>(slot * 3 / 2);
        stopper = Keeper.keep(this);
    }

    /**
     * push未来某时刻需要处理的数据
     */
    public void push(long time, D data) {
        int ringSlot = (int) ((time / unit) % slot);
        List<D> ringItemData = CollectionUtils.putIfAbsent(ringData, ringSlot, new ArrayList<>());
        ringItemData.add(data);
    }

    /**
     * 停止轮转
     *
     * @return 未处理的数据
     */
    public List<D> stop() {
        stopper.stop();
        stopper = null;

        //返回未处理的数据
        return ringData.values().stream().reduce((l1, l2) -> {
            List<D> list = new ArrayList<>(l1.size() + l2.size());
            list.addAll(l1);
            list.addAll(l2);
            return list;
        }).orElse(Collections.emptyList());
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
            ExceptionUtils.throwExt(e);
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
