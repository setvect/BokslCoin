package com.setvect.bokslcoin.autotrading.etc;

import com.setvect.bokslcoin.autotrading.util.MathUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaiTest {
    @Test
    public void test() {
        double[] t = {1002, 1000, 1001, 1000, 999, 980, 1100, 700};

        List<Double> priceList = Arrays.stream(t).boxed().collect(Collectors.toList());
        int maWindowSize = 3;
        double current = MathUtil.getMa(priceList, 0, maWindowSize);
        System.out.printf("current: %f\n", current);
        System.out.println("---------------------");
        int iterSize = 5;
        List<Double> beforeMa = MathUtil.getAvgValues(priceList, 1, maWindowSize, iterSize);

        for (Double maValue : beforeMa) {
            System.out.printf("maValue: %f\n", maValue);
        }


        System.out.println("---------------------");
        double minPrice = MathUtil.getContinuesMin(beforeMa);
        System.out.printf("minPrice: %f\n", minPrice);

        double maxPrice = MathUtil.getContinuesMax(beforeMa);
        System.out.printf("maxPrice: %f\n", maxPrice);
        double yield = MathUtil.getYield(current, minPrice);
        System.out.printf("수익률: %f%%\n", yield * 100);
    }
}
