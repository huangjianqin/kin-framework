package org.kin.framework.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author huangjianqin
 * @date 2019/7/31
 */
public enum ByteUnit {
    /**
     * b
     */
    BIT("bit") {
        @Override
        int power() {
            return 0;
        }
    },
    /**
     * B
     */
    BYTE("B") {
        @Override
        int power() {
            return BIT.power() + 3;
        }
    },
    /**
     * KB
     */
    KILOBYTE("KB") {
        @Override
        int power() {
            return BYTE.power() + 10;
        }
    },
    /**
     * MB
     */
    MEGABYTE("MB") {
        @Override
        int power() {
            return BYTE.power() + 20;
        }
    },
    /**
     * GB
     */
    GIGABYTE("GB") {
        @Override
        int power() {
            return BYTE.power() + 30;
        }
    },
    /**
     * TB
     */
    TERABYTE("TB") {
        @Override
        int power() {
            return BYTE.power() + 40;
        }
    },
    /**
     * PB
     */
    PETABYTE("PB") {
        @Override
        int power() {
            return BYTE.power() + 50;
        }
    },

    ;
    private static final ByteUnit[] VALUES = values();

    /** 单位缩写 */
    private final String abbreviation;

    ByteUnit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    /**
     * 返回2的n次方
     */
    abstract int power();

    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * 单位转换格式
     *
     * @param source     字节数
     * @param sourceUnit 原单位
     * @return 新单位所表示的字节数
     */
    public double convert(long source, ByteUnit sourceUnit) {
        return convert(source, sourceUnit, this);
    }

    /**
     * 单位转换格式
     *
     * @param source     字节数
     * @param sourceUnit 原单位
     * @return 新单位所表示的字节数
     */
    public BigDecimal convert(BigDecimal source, ByteUnit sourceUnit) {
        return convert(source, sourceUnit, this);
    }

    //------------------------------------------------------------------------------------------------------------------

    public static double convert(long source, ByteUnit sourceUnit, ByteUnit targetUnit) {
        return convert(BigDecimal.valueOf(source), sourceUnit, targetUnit).doubleValue();
    }

    public static double byte2Other(long source, ByteUnit targetUnit) {
        return convert(BigDecimal.valueOf(source), BYTE, targetUnit).doubleValue();
    }

    public static BigDecimal byte2Other(BigDecimal source, ByteUnit targetUnit) {
        return convert(source, BYTE, targetUnit);
    }

    public static BigDecimal convert(BigDecimal source, ByteUnit sourceUnit, ByteUnit targetUnit) {
        int dis = targetUnit.power() - sourceUnit.power();
        BigDecimal base = BigDecimal.valueOf(2);
        if (dis >= 0) {
            base = base.pow(dis);
            //保留两位小数
            return source.divide(base, 2, RoundingMode.DOWN);
        } else {
            base = base.pow(-dis);
            return source.multiply(base);
        }
    }

    /**
     * 字节显示格式化
     */
    public static String format(long source) {
        return format(source, BYTE);
    }

    /**
     * 字节显示格式化
     */
    public static String format(long source, ByteUnit sourceUnit) {
        BigDecimal decimal = BigDecimal.valueOf(source);
        ByteUnit formatUnit = sourceUnit;
        //查找原单位index
        int idx = -1;
        for (int i = 0; i < VALUES.length; i++) {
            if (sourceUnit == VALUES[i]) {
                idx = i;
                break;
            }
        }

        BigDecimal base = BigDecimal.valueOf(2);
        //找到一个单位转换为显示>0, 并返回对应的字符串
        for (int i = idx + 1; i < VALUES.length; i++) {
            ByteUnit unit = VALUES[i];
            int power = unit.power() - formatUnit.power();
            //保留两位小数
            BigDecimal nextDecimal = decimal.divide(base.pow(power), 2, RoundingMode.DOWN);
            if (nextDecimal.longValue() <= 0) {
                return decimal.doubleValue() + formatUnit.getAbbreviation();
            }
            decimal = nextDecimal;
            formatUnit = unit;
        }

        return source + formatUnit.getAbbreviation();
    }
}
