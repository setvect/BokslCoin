package com.setvect.bokslcoin.autotrading.util;


import org.junit.jupiter.api.Test;

public class LimitedSizeQueueTest {
    @Test
    public void testAdd() {
        LimitedSizeQueue<Integer> limit = new LimitedSizeQueue<>(10);

        for (int i = 0; i < 15; i++) {
            limit.add(i);
        }

        System.out.println(limit);
    }

    @Test
    public void testAddIndex() {
        LimitedSizeQueue<Integer> limit = new LimitedSizeQueue<>(10);

        for (int i = 0; i < 15; i++) {
            limit.add(0, i);
        }

        System.out.println(limit);
    }
}
