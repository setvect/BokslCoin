package com.setvect.bokslcoin.autotrading.util;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * 복슬코인에서 사용하는 수학 계산식
 */
public class MathUtil {

    /**
     * 배열에 일부 영역을 선택하여 평균 값을 구함
     *
     * @param values 대상 값
     * @param start  시작 지점(0부터)
     * @param limit  시작 지점 기준
     * @return 평균값
     */
    public static double getMa(List<Double> values, int start, int limit) {
        if (values.size() < start + limit) {
            String message = String.format("값을 계산하기 위한 리스트 사이즈가 작습니다. 리스트 사이즈: %d, start: %d, limit: %d", values.size(), start, limit);
            throw new RuntimeException(message);
        }

        OptionalDouble avg = values.stream().skip(start).limit(limit).mapToDouble(a -> a).average();
        if (avg.isPresent()) {
            return avg.getAsDouble();
        }
        return 0;
    }

    /**
     * Window 사이즈만 큼 offset를 하나씩 증가 시켜 iterSize 만큼 평균값을 구함
     *
     * @param priceList    값이 들어 있는 배열
     * @param offset       시작 offset 위치(0부터
     * @param maWindowSize 윈도우 사이즈
     * @param iterSize     총
     * @return 평균값
     */
    public static List<Double> getAvgValues(List<Double> priceList, int offset, int maWindowSize, int iterSize) {
        List<Double> beforeMaWindow = new ArrayList<>();
        for (int i = 0; i < iterSize; i++) {
            double ma = getMa(priceList, i + offset, maWindowSize);
            beforeMaWindow.add(ma);
        }
        return beforeMaWindow;
    }


    /**
     * 연속된 가장 작은 값
     *
     * @param beforeMa
     * @return
     */
    public static double getContinuesMin(List<Double> beforeMa) {
        double min = beforeMa.get(0);
        for (int i = 1; i < beforeMa.size(); i++) {
            if (min >= beforeMa.get(i)) {
                min = beforeMa.get(i);
            } else {
                return min;
            }
        }
        return min;
    }

    /**
     * 연속된 가장 큰 값
     *
     * @param beforeMa
     * @return
     */
    public static double getContinuesMax(List<Double> beforeMa) {
        double max = beforeMa.get(0);
        for (int i = 1; i < beforeMa.size(); i++) {
            if (max <= beforeMa.get(i)) {
                max = beforeMa.get(i);
            } else {
                return max;
            }
        }
        return max;
    }

    /**
     * 예시(
     * 반환값: 0.1 → 10%
     * 반환값: -0.2 → -20%
     * 반환값: 1.2 → 120%
     *
     * @param current 현재값
     * @param base    기준값
     * @return 수익률
     */
    public static double getYield(double current, double base) {
        return current / base - 1;
    }
}
