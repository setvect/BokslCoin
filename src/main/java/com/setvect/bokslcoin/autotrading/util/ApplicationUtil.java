package com.setvect.bokslcoin.autotrading.util;

import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * 어플리케이션의 의존적인 유틸성 메소드
 */
public class ApplicationUtil {
    public static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#.############");

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

    /**
     * @param value 값
     * @return 지수형태가 아닌 소수점으로 표현
     */
    public static String toNumberString(double value) {
        return NUMBER_FORMAT.format(value);
    }

    /**
     * UTC를 기준으로 범위를 만들고, 로컬 컴퓨터(서버)에 Zone을 적용시켜 날짜 범위 반환
     *
     * @param fromTimeStr 시작 시간 HH:mm:ss
     * @param toTimeStr   종료 시간 HH:mm:ss
     * @return 오늘 날짜로 입력 시간으로 범위를 설정함
     */
    public static DateRange getDateRange(String fromTimeStr, String toTimeStr) {
        return getDateRange(LocalDate.now(), fromTimeStr, toTimeStr);
    }

    /**
     * UTC를 기준으로 범위를 만들고, 로컬 컴퓨터(서버)에 Zone을 적용시켜 날짜 범위 반환
     *
     * @param baseDate    기준 날짜
     * @param fromTimeStr 시작 시간 HH:mm:ss
     * @param toTimeStr   종료 시간 HH:mm:ss
     * @return 오늘 날짜로 입력 시간으로 범위를 설정함
     */
    public static DateRange getDateRange(LocalDate baseDate, String fromTimeStr, String toTimeStr) {
        LocalTime fromTime = DateUtil.getLocalTime(fromTimeStr);
        LocalTime toTime = DateUtil.getLocalTime(toTimeStr);

        // UTC 기준으로 날짜
        return getDateRange(baseDate, fromTime, toTime);
    }

    /**
     * UTC를 기준으로 범위를 만들고, 로컬 컴퓨터(서버)에 Zone을 적용시켜 날짜 범위 반환
     *
     * @param baseDate 기준 날짜
     * @param fromTime 시작 시간
     * @param toTime   종료 시간
     * @return 오늘 날짜로 입력 시간으로 범위를 설정함
     */

    public static DateRange getDateRange(LocalDate baseDate, LocalTime fromTime, LocalTime toTime) {
        ZonedDateTime fromUtc = ZonedDateTime.of(baseDate.getYear(), baseDate.getMonthValue(), baseDate.getDayOfMonth(), fromTime.getHour(), fromTime.getMinute(), fromTime.getSecond(), 0, ZoneId.of("UTC"));
        ZonedDateTime toUtc = ZonedDateTime.of(fromUtc.toLocalDateTime(), ZoneId.of("UTC")).withHour(toTime.getHour()).withMinute(toTime.getMinute()).withSecond(toTime.getSecond());

        // 로컬 타임존으로 변경
        LocalDateTime fromLocal = fromUtc.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toLocal = toUtc.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();

        DateRange range = new DateRange(fromLocal, toLocal);
        return range;
    }

    /**
     * @param values
     * @return 최대 낙폭 계산 - MDD(Max. Draw Down)
     */
    public static double getMdd(List<Double> values) {
        double highValue = 0;
        double mdd = 0;
        for (Double v : values) {
            if (highValue < v) {
                highValue = v;
            } else {
                mdd = Math.min(mdd, v / highValue - 1);
            }
        }
        return mdd;
    }

    public static String request(String url, HttpRequestBase request) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();

        HttpResponse response = client.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        String responseText = EntityUtils.toString(entity, "UTF-8");

        if (statusCode != 200 && statusCode != 201) {
            String message = String.format("Error, Status: %d, URL: %s, Message: %s", statusCode, url, responseText);
            throw new RuntimeException(message);
        }
        return responseText;
    }

    public static String getQueryString(Map<String, String> params) {
        String queryString = params.entrySet().stream()
                .map(p -> p.getKey() + "=" + urlEncodeUTF8(p.getValue()))
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");
        return queryString;
    }


    @SneakyThrows
    private static String urlEncodeUTF8(String s) {
        return URLEncoder.encode(s, "UTF-8");
    }
}
