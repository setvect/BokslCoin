package com.setvect.bokslcoin.autotrading.etc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class DateTest {
    public static void main(String[] args){
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(30);
        long dayCount = ChronoUnit.DAYS.between(from, to);
        System.out.println(dayCount);
        System.out.println("----------");

        // 시간을 UTC로 변환
        LocalDateTime aaa = from.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        System.out.println(aaa);
    }
}
