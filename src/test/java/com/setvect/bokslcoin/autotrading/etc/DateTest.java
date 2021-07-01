package com.setvect.bokslcoin.autotrading.etc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateTest {
    public static void main(String[] args) throws IOException {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(30);
        long dayCount = ChronoUnit.DAYS.between(from, to);
        System.out.println(dayCount);
    }
}
