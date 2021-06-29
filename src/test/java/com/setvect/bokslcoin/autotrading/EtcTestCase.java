package com.setvect.bokslcoin.autotrading;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EtcTestCase {
    @Test
    public void testLocalDateTimeFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime aa = LocalDateTime.parse("2021-06-06T03:50:00", formatter);
        System.out.println(aa);
    }

    @Test
    public void testLocalDateTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate aa = LocalDate.parse("20210606", formatter);
        System.out.println(aa);
    }
}
