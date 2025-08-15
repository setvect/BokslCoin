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
        RANGE_LIST.add(new RangeItem(1_000_000, 1000.0));
        RANGE_LIST.add(new RangeItem(500_000, 500.0));
        RANGE_LIST.add(new RangeItem(100_000, 100.0));
        RANGE_LIST.add(new RangeItem(50_000, 50.0));
        RANGE_LIST.add(new RangeItem(10_000, 10.0));
        RANGE_LIST.add(new RangeItem(5_000, 5.0));
        RANGE_LIST.add(new RangeItem(1_000, 1.0));
        RANGE_LIST.add(new RangeItem(100, 1.0));
        RANGE_LIST.add(new RangeItem(10, 0.1));
        RANGE_LIST.add(new RangeItem(1, 0.01));
        RANGE_LIST.add(new RangeItem(0.1, 0.001));
        RANGE_LIST.add(new RangeItem(0.01, 0.0001));
        RANGE_LIST.add(new RangeItem(0.001, 0.00001));
        RANGE_LIST.add(new RangeItem(0.0001, 0.000001));
        RANGE_LIST.add(new RangeItem(0.00001, 0.0000001));
        RANGE_LIST.add(new RangeItem(0, 0.00000001));
    }

    /**
     * 가격에 맞는 호가 단위 제공. 호가 단위를 초과 하는 값은 버림<br>
     * ex)
     * 112,230 -> 112,200
     * 152.4 -> 152
     * 참고: https://upbit.com/service_center/notice?id=5310
     *
     * @param price 가격
     * @return 호가
     */
    public static double askPrice(double price) {
        for (RangeItem r : RANGE_LIST) {
            if (price >= r.upperValue) {
                // 더 정밀한 계산을 위해 긴 소수점 처리
                double multiplier = 1.0 / r.unit;
                return Math.floor(price * multiplier) / multiplier;
            }
        }
        return -1.0;
    }

    @AllArgsConstructor
    @ToString
    static class RangeItem {
        private double upperValue;
        private double unit;
    }
}