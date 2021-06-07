package com.setvect.bokslcoin.autotrading.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ApplicationUtils {

    public static final String yyyyMMdd = "yyyyMMdd";
    public static final String yyyy_MM_dd = "yyyy-MM-dd";
    public static final String yyyy_MM_ddTHH_mm_ss = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String yyyy_MM_ddTHH_mm_ssZ = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String HHmmss = "HHmmss";


    public static LocalDateTime getLocalDateTime(String text, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(text, formatter);
    }

    public static LocalDate getLocalDate(String text, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(text, formatter);
    }

    public static LocalTime getLocalTime(String text, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalTime.parse(text, formatter);
    }

    public static String formatFromLocalDateTime(LocalDateTime localDateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(formatter);
    }


    /**
     * askPrice가 100,521
     * priceUnit값이 500원이면
     * 100,500반환
     *
     * @param askPrice  호가
     * @param priceUnit 호가 단위
     * @return 호가에서 호가 단위를 절사해 반환
     */
    public static String applyAskPrice(double askPrice, int priceUnit) {
        double remain = askPrice % priceUnit;
        BigDecimal price = new BigDecimal(askPrice - remain);
        return price.toString();
    }
}
