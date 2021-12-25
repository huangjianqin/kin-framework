package org.kin.framework.collection;

import org.kin.framework.utils.SPI;

import java.util.Map;

/**
 * map工厂类
 *
 * @author huangjianqin
 * @date 2021/12/25
 */
@SPI
@FunctionalInterface
public interface MapFactory<M extends Map<?, ?>> extends Factory {
    /**
     * 创建{@link Map}实例
     */
    M newMap();
}
