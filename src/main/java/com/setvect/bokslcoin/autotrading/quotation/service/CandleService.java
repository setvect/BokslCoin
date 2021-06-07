package com.setvect.bokslcoin.autotrading.quotation.service;

import com.google.gson.reflect.TypeToken;
import com.setvect.bokslcoin.autotrading.ConnectionInfo;
import com.setvect.bokslcoin.autotrading.common.service.CommonFeature;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.util.ApplicationUtils;
import com.setvect.bokslcoin.autotrading.util.GsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
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
     * 분(Minute) 캔들
     *
     * @param unit   분 단위. 가능한 값 : 1, 3, 5, 15, 10, 30, 60, 240
     * @param market 마켓 코드 (ex. KRW-BTC)
     * @param to     마지막 캔들 시각 (exclusive).
     * @param count  캔들 개수(최대 200개까지 요청 가능)
     * @return
     */
    public List<CandleMinute> callMinute(int unit, String market, LocalDateTime to, int count) {
        try {
            String url = URL_MINUTES.replace("{unit}", String.valueOf(unit));
            Map<String, String> params = new HashMap<>();

            params.put("market", market);
            params.put("to", ApplicationUtils.formatFromLocalDateTime(to, ApplicationUtils.yyyy_MM_ddTHH_mm_ssZ));
            params.put("count", String.valueOf(count));

            String jsonResult = CommonFeature.requestApi(url, params, connectionInfo);
            List<CandleMinute> candles = GsonUtil.GSON.fromJson(jsonResult, new TypeToken<List<CandleMinute>>() {
            }.getType());

            return candles;
        } catch (IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


}
