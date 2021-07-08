package com.setvect.bokslcoin.autotrading.backtest;

import com.google.gson.Gson;
import com.setvect.bokslcoin.autotrading.TestCommonUtil;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.model.CandleMinute;
import com.setvect.bokslcoin.autotrading.quotation.CandleService;
import com.setvect.bokslcoin.autotrading.util.DateUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 크롤링
 */
@SpringBootTest
@ActiveProfiles("local")
public class Crawling {
    private static final File SAVE_DIR = new File("./craw-data");
    private static final File SAVE_DIR_MINUTE = new File("./craw-data/minute");

    @Autowired
    private CandleService candleService;

    @Test
    public void 일봉수집() throws IOException, InterruptedException {
        makeSaveDir(SAVE_DIR);
        String market = "KRW-BTC";

        Gson gson = TestCommonUtil.getGson();
        List<CandleDay> acc = new ArrayList<>();


        LocalDateTime to = null;
        for (int i = 0; i < 300; i++) {
            List<CandleDay> data = candleService.getDay(market, 200, to);

            if (data.isEmpty()) {
                break;
            }
            acc.addAll(data);
            to = data.get(data.size() - 1).getCandleDateTimeUtc();
            System.out.println(i + " 번째");

            // API 횟수 제한 때문에 딜레이 적용
            TimeUnit.MILLISECONDS.sleep(300);
        }
        File saveFile = new File(SAVE_DIR, market + ".json");
        try (FileWriter writer = new FileWriter(saveFile);) {
            gson.toJson(acc, writer);
        }
        System.out.printf("저장: %s%n끝%n", saveFile.getAbsolutePath());
    }

    @Test
    public void 분봉수집() throws IOException, InterruptedException {
        makeSaveDir(SAVE_DIR_MINUTE);
        List<String> marketList = Arrays.asList("KRW-BTC");

        for (String market : marketList) {
            List<CandleMinute> acc = new ArrayList<>();
            LocalDateTime to = null;
            int month = LocalDateTime.now(ZoneId.of("UTC")).getMonth().getValue();
            for (int i = 0; i < 13_000; i++) {
                List<CandleMinute> data = candleService.getMinute(1, market, 200, to);
                if (data.isEmpty()) {
                    break;
                }
                for (CandleMinute d : data) {
                    LocalDateTime candleDateTimeUtc = d.getCandleDateTimeUtc();
                    if (candleDateTimeUtc.getMonth().getValue() != month) {
                        saveFileMinute(market, acc);
                        acc.clear();
                        month = candleDateTimeUtc.getMonth().getValue();
                    }
                    acc.add(d);
                }
                to = data.get(data.size() - 1).getCandleDateTimeUtc();
                System.out.println(i + " 번째");
                // API 횟수 제한 때문에 딜레이 적용
                TimeUnit.MILLISECONDS.sleep(180);
            }
            saveFileMinute(market, acc);
        }
    }

    private void saveFileMinute(String market, List<CandleMinute> acc) throws IOException {
        if (acc.isEmpty()) {
            return;
        }
        Gson gson = TestCommonUtil.getGson();
        String fileName = String.format("%s-minute(%s).json", market, DateUtil.format(acc.get(0).getCandleDateTimeUtc(), "yyyy-MM"));
        File saveFile = new File(SAVE_DIR_MINUTE, fileName);
        try (FileWriter writer = new FileWriter(saveFile);) {
            gson.toJson(acc, writer);
        }
        System.out.printf("저장: %s%n", saveFile.getAbsolutePath());
    }

    private void makeSaveDir(File dir) {
        if (dir.exists()) {
            return;
        }
        dir.mkdirs();
    }
}

