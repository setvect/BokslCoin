package com.setvect.bokslcoin.autotrading.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class DateUtil {

    public static final String yyyyMMdd = "yyyyMMdd";
    public static final String yyyy_MM_dd = "yyyy-MM-dd";

    public static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
    public static final String yyyy_MM_ddTHH_mm_ss = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String yyyy_MM_ddTHH_mm_ssZ = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String HHmmss = "HHmmss";
    public static final String HH_mm_ss = "HH:mm:ss";

    public static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#.############");


    public static LocalDateTime getLocalDateTime(String text, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(text, formatter);
    }

    /**
     * @param dateStr yyyy-MM-dd 형태
     * @return LocalDAte
     */
    public static LocalDate getLocalDate(String dateStr) {
        return getLocalDate(dateStr, yyyy_MM_dd);
    }

    public static LocalDate getLocalDate(String dateStr, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(dateStr, formatter);
    }

    public static LocalDateTime getLocalDateTime(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyy_MM_ddTHH_mm_ss);
        return LocalDateTime.parse(dateStr, formatter);
    }

    public static LocalTime getLocalTime(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(HH_mm_ss);
        return LocalTime.parse(timeStr, formatter);
    }

    public static LocalTime getLocalTime(String timeStr, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalTime.parse(timeStr, formatter);
    }

    /**
     * @param localDateTime 날짜시간
     * @return yyyy-MM-dd 형태로 반환
     */
    public static String format(LocalDateTime localDateTime) {
        return format(localDateTime, yyyy_MM_dd);
    }

    public static String format(LocalDateTime localDateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return localDateTime.format(formatter);
    }

    public static String formatDateTime(LocalDateTime localDateTime) {
        return format(localDateTime, yyyy_MM_dd_HH_mm_ss);
    }

    public static String format(LocalTime localTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return localTime.format(formatter);
    }


    public static LocalDateTime convert(long timeInMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), TimeZone.getDefault().toZoneId());
    }

}
