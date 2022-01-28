package com.setvect.bokslcoin.autotrading.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

/**
 * 날짜의 범위를 나타내줌 <br>
 * 날짜 범위 검색에 필요한 파라미터 역활을 할 수 있음
 */
public class DateRange {
    /**
     * 시작 날짜
     */
    private LocalDateTime from;

    /**
     * 종료 날짜
     */
    private final LocalDateTime to;

    /**
     * 기간 제한 없는 날짜 시작일
     */
    public static final String UNLIMITE_DATE_START = "1990-01-01";

    /**
     * 기간 제한 없는 날짜 종료일
     */
    public static final String UNLIMITE_DATE_END = "2100-12-31";

    /**
     * @return 1990-01-01 ~ 2100-12-31 날짜 범위 리턴
     * @see #UNLIMITE_DATE_START
     * @see #UNLIMITE_DATE_END
     */
    public static DateRange getMaxRange() {
        return new DateRange(UNLIMITE_DATE_START, UNLIMITE_DATE_END);
    }

    /**
     * 오늘 날짜를 기준으로 해서 차이 값을 생성 한다.
     *
     * @param diff
     */
    public DateRange(int diff) {
        // 양수()
        if (diff > 0) {
            from = LocalDateTime.now();
            to = from.plusDays(diff);
        }
        // 음수
        else {
            to = LocalDateTime.now();
            from = from.plusDays(diff);
        }
    }

    /**
     * 날짜 범위를 해당 년도의 달에 1부터 그달의 마지막으로 한다.
     *
     * @param year 년도
     */
    public DateRange(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);

        // 해당 달의 맨 끝에 날짜로 가기위해서
        cal.set(Calendar.DATE, 1);
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.DATE, -1);
        to = DateUtil.convert(cal.getTimeInMillis());

        cal.set(Calendar.DATE, 1);
        from = DateUtil.convert(cal.getTimeInMillis());
    }

    /**
     * 날짜영역 객체 생성. 기본 날짜 포맷 (yyyy-MM-dd)으로 날짜 변환
     *
     * @param from 시작날짜
     * @param to   종료날짜
     */
    public DateRange(String from, String to) {
        this(from, to, "yyyy-MM-dd'T'HH:mm:ss");
    }

    /**
     * 날짜영역 객체 생성. 기본 날짜 포맷 (yyyy-MM-dd)으로 날짜 변환
     *
     * @param from   시작날짜
     * @param to     종료날짜
     * @param format 날짜 패턴 "yyyy, MM, dd, HH, mm, ss and more"
     */
    public DateRange(String from, String to, String format) {
        this.from = DateUtil.getLocalDateTime(from, format);
        this.to = DateUtil.getLocalDateTime(to, format);
    }

    /**
     * 날짜영역 객체 생성.
     *
     * @param from 시작일
     * @param to   종료일
     */
    public DateRange(LocalDateTime from, LocalDateTime to) {
        this.from = from;
        this.to = to;
    }

    /**
     * @return 종료날짜를 리턴합니다.
     */
    public LocalDateTime getTo() {
        return to;
    }

    /**
     * @return 시작날짜를 리턴합니다.
     */
    public LocalDateTime getFrom() {
        return from;
    }

    /**
     * @return 종료날짜를 "yyyy-MM-dd" 형태로 리턴합니다.
     */
    public String getToDateFormat() {
        return DateUtil.format(to, "yyyy-MM-dd");
    }

    /**
     * @return 시작날짜를 "yyyy-MM-dd" 형태로 리턴합니다.
     */
    public String getFromDateFormat() {
        return DateUtil.format(from, "yyyy-MM-dd");
    }

    /**
     * @return 종료날짜를 "yyyy-MM-dd HH:mm:ss" 형태로 리턴합니다.
     */
    public String getToDateTimeFormat() {
        return DateUtil.format(to, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * @return 시작날짜를 "yyyy-MM-dd HH:mm:ss" 형태로 리턴합니다.
     */
    public String getFromDateTimeFormat() {
        return DateUtil.format(from, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * @param format 날짜 패턴 "yyyy, MM, dd, HH, mm, ss and more"
     * @return 종료날짜를 포맷 형태로 리턴합니다.
     */
    public String getToDateTimeFormat(String format) {
        return DateUtil.format(to, format);
    }

    /**
     * @param format 날짜 패턴 "yyyy, MM, dd, HH, mm, ss and more"
     * @return 종료날짜를 포맷 형태로 리턴합니다.
     */
    public String getFromDateTimeFormat(String format) {
        return DateUtil.format(from, format);
    }


    /**
     * 시작과 끝을 포함한 between 검사
     *
     * @param dateTime 검사할 날짜시간
     * @return 두 날짜 사이에 있는지에 있으면 true
     */
    public boolean isBetween(LocalDateTime dateTime) {
        return from.isBefore(dateTime) && to.isAfter(dateTime) || from.isEqual(dateTime) || to.isEqual(dateTime);
    }

    @Override
    public String toString() {
        return DateUtil.formatDateTime(from) + " ~ " + DateUtil.formatDateTime(to);
    }

    public long getDiffMinute() {
        Duration dur = Duration.between(from, to);
        return (dur.getSeconds() / 60);
    }

    /**
     * @return 날짜 차이
     */
    public long getDiffDays() {
        return ChronoUnit.DAYS.between(from, to);
    }
}
