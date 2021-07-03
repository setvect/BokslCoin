package com.setvect.bokslcoin.autotrading.quotation;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.common.ApiCaller;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandleService {
    private static final String URL_MINUTES = "/v1/candles/minutes/{unit}";
    private static final String URL_DAYS = "/v1/candles/days";

    private final ConnectionInfo connectionInfo;


    /**
     * 현재 시간 기준으로 분(Minute) 캔들
     *
     * @param unit   분 단위. 가능한 값 : 1, 3, 5, 15, 10, 30, 60, 240
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @return 분(Minute) 캔들(최근이 순서대로)
     */
    public CandleMinute getMinute(int unit, String market) {
        return getMinute(unit, market, 1, null).get(0);
    }

    /**
     * 현재 시간 기준으로 분(Minute) 캔들
     *
     * @param unit   분 단위. 가능한 값 : 1, 3, 5, 15, 10, 30, 60, 240
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @param count  캔들 개수(최대 200개까지 요청 가능)
     * @return 분(Minute) 캔들(최근 순서대로)
     */
    public List<CandleMinute> getMinute(int unit, String market, int count) {
        return getMinute(unit, market, count, null);
    }

    /**
     * 분(Minute) 캔들
     *
     * @param unit   분 단위. 가능한 값 : 1, 3, 5, 15, 10, 30, 60, 240
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @param count  캔들 개수(최대 200개까지 요청 가능)
     * @param to     마지막 캔들 시각 (exclusive).
     * @return 분 캔들(최근 순서대로)
     */
    public List<CandleMinute> getMinute(int unit, String market, int count, LocalDateTime to) {
        String url = URL_MINUTES.replace("{unit}", String.valueOf(unit));
        Map<String, String> params = new HashMap<>();

        params.put("market", market);
        params.put("count", String.valueOf(count));
        if (to != null) {
            params.put("to", DateUtil.format(to, DateUtil.yyyy_MM_ddTHH_mm_ssZ));
        }

        String jsonResult = ApiCaller.requestApi(url, params, connectionInfo);
        List<CandleMinute> candles = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<CandleMinute>>() {
        }.getType());
        return candles;
    }

    /**
     * 현재 시간 기준으로 일(Day) 캔들
     *
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @param count  캔들 개수(최대 200개까지 요청 가능)
     * @return 일단위 캔들(최근 순서대로)
     */
    public List<CandleDay> getDay(String market, int count) {
        return getDay(market, count, null);
    }

    /**
     * 일(Day) 캔들
     *
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @param count  캔들 개수(최대 200개까지 요청 가능)
     * @param to     마지막 캔들 시각 (exclusive).
     * @return 일단위 캔들(최근 순서대로)
     */
    public List<CandleDay> getDay(String market, int count, LocalDateTime to) {
        Map<String, String> params = new HashMap<>();

        params.put("market", market);
        params.put("count", String.valueOf(count));
        if (to != null) {
            params.put("to", DateUtil.format(to, DateUtil.yyyy_MM_ddTHH_mm_ssZ));
        }

        String jsonResult = ApiCaller.requestApi(URL_DAYS, params, connectionInfo);
        List<CandleDay> candles = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<CandleDay>>() {
        }.getType());
        return candles;
    }
}
