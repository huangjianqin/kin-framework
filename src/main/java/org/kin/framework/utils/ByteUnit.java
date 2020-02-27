package org.kin.framework.utils;

import java.math.BigDecimal;

/**
 * @author huangjianqin
 * @date 2019/7/31
 */
public enum ByteUnit {
    /**
     * b
     */
    BIT {
        @Override
        public int pow() {
            return 0;
        }
    },
    /**
     * B
     */
    BYTE {
        @Override
        public int pow() {
            return BIT.pow() + 3;
        }
    },
    /**
     * KB
     */
    KILOBYTE {
        @Override
        public int pow() {
            return BYTE.pow() + 10;
        }
    },
    /**
     * MB
     */
    MEGABYTE {
        @Override
        public int pow() {
            return BYTE.pow() + 20;
        }
    },
    /**
     * GB
     */
    GIGABYTE {
        @Override
        public int pow() {
            return BYTE.pow() + 30;
        }
    },
    /**
     * TB
     */
    TERABYTE {
        @Override
        public int pow() {
            return BYTE.pow() + 40;
        }
    },
    /**
     * PB
     */
    PETABYTE {
        @Override
        public int pow() {
            return BYTE.pow() + 50;
        }
    },

    ;

    public abstract int pow();

    public String convert(long source, ByteUnit sourceUnit) {
        return convert(source, sourceUnit, this);
    }

    public String convert(double source, ByteUnit sourceUnit) {
        return convert(source, sourceUnit, this);
    }

    public String convert(BigDecimal source, ByteUnit sourceUnit) {
        return convert(source, sourceUnit, this);
    }

    //------------------------------------------------------------------------------------------------------------------

    public static String convert(long source, ByteUnit sourceUnit, ByteUnit targetUnit) {
        return convert(BigDecimal.valueOf(source), sourceUnit, targetUnit);
    }

    public static String convert(double source, ByteUnit sourceUnit, ByteUnit targetUnit) {
        return convert(BigDecimal.valueOf(source), sourceUnit, targetUnit);
    }

    public static String convert(BigDecimal source, ByteUnit sourceUnit, ByteUnit targetUnit) {
        int dis = targetUnit.pow() - sourceUnit.pow();
        BigDecimal base = BigDecimal.valueOf(2);
        if (dis >= 0) {
            base = base.pow(dis);
            return source.divide(base, BigDecimal.ROUND_DOWN).toString();
        } else {
            base = base.pow(-dis);
            return source.multiply(base).toString();
        }
    }
}
