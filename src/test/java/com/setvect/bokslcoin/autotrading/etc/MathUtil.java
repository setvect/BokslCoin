package com.setvect.bokslcoin.autotrading.etc;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 기하평균
 */
public class MathUtil {
    @Test
    public void test() {
        List<Double> list = Arrays.asList(1.0, -0.5, 0.1);

        double multi = 1;
        for (Double v : list) {
            multi *= (1 + v);
        }
        double result = Math.pow(multi, 1.0 / list.size()) - 1;
        System.out.println(result * list.size());
    }
}
