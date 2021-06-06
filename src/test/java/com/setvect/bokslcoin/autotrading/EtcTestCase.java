package com.setvect.bokslcoin.autotrading;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EtcTestCase {
    public static void main(String[] args) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime aa = LocalDateTime.parse("2021-06-06T03:50:00", formatter);
        System.out.println(aa);
    }
}
