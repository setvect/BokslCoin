package com.setvect.bokslcoin.autotrading.etc;

import com.setvect.bokslcoin.autotrading.algorithm.AskPriceRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AskPriceRangeTest {
    @Test
    public void test() {
        Assertions.assertEquals(11, AskPriceRange.askPrice(11));
        Assertions.assertEquals(2000000, AskPriceRange.askPrice(2000050));
        Assertions.assertEquals(3000000, AskPriceRange.askPrice(3000950));
        Assertions.assertEquals(1955000, AskPriceRange.askPrice(1955000));
        Assertions.assertEquals(1955000, AskPriceRange.askPrice(1955055));
        Assertions.assertEquals(200050, AskPriceRange.askPrice(200080));
        Assertions.assertEquals(15.2, AskPriceRange.askPrice(15.29), 0.000001);
        Assertions.assertEquals(9.22, AskPriceRange.askPrice(9.225), 0.000001);
    }
}
