package com.setvect.bokslcoin.autotrading.algorithm;

import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * 호가 단위를 맞추기위한 기능 제공
 */
public class AskPriceRange {

    private static final List<RangeItem> RANGE_LIST = new ArrayList<>();

    static {
        RANGE_LIST.add(new RangeItem(2_000_000, 1000.0));
        RANGE_LIST.add(new RangeItem(1_000_000, 500.0));
        RANGE_LIST.add(new RangeItem(500_000, 100.0));
        RANGE_LIST.add(new RangeItem(100_000, 50.0));
        RANGE_LIST.add(new RangeItem(10_000, 10.0));
        RANGE_LIST.add(new RangeItem(1_000, 5.0));
        RANGE_LIST.add(new RangeItem(100, 1.0));
        RANGE_LIST.add(new RangeItem(10, 0.1));
        RANGE_LIST.add(new RangeItem(0, 0.01));
    }

    /**
     * 가격에 맞는 호가 단위 제공. 호가 단위를 초과 하는 값은 버림<br>
     * ex)
     * 112,230 -> 112,200
     * 152.4 -> 152
     * 참고: https://docs.upbit.com/docs/market-info-trade-price-detail
     *
     * @param price 가격
     * @return 호가
     */
    public static double askPrice(double price) {
        for (RangeItem r : RANGE_LIST) {
            if (price >= r.upperValue) {
                long value = (long) (price * 100);
                long temp = value - value % (long) (r.unit * 100);
                double v = temp * 0.01;
                return v;
            }
        }
        return -1.0;
    }

    @AllArgsConstructor
    @ToString
    static class RangeItem {
        private int upperValue;
        private double unit;
    }
}
