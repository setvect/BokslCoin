package com.setvect.bokslcoin.autotrading.backtest;

import com.google.gson.Gson;
import com.setvect.bokslcoin.autotrading.TestCommonUtil;
import com.setvect.bokslcoin.autotrading.model.CandleDay;
import com.setvect.bokslcoin.autotrading.quotation.service.CandleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 크롤링
 */
@SpringBootTest
@ActiveProfiles("local")
public class Crawling {
    private static final File SAVE_DIR = new File("./craw-data");

    @Autowired
    private CandleService candleService;

    @Test
    public void 일봉수집() throws IOException, InterruptedException {
        makeSaveDir();
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
            to = data.get(data.size() - 1).getCandleDateTimeKst().minusDays(1);
            System.out.println(i +" 번째");
            
            // API 횟수 제한 때문에 딜레이 적용
            TimeUnit.MILLISECONDS.sleep(300);
        }
        File saveFile = new File(SAVE_DIR, market + ".json");
        try (FileWriter writer = new FileWriter(saveFile);) {
            gson.toJson(acc, writer);
        }
        System.out.printf("저장: %s%n끝%n", saveFile.getAbsolutePath());
    }

    private void makeSaveDir() {
        if (SAVE_DIR.exists()) {
            return;
        }
        SAVE_DIR.mkdir();
    }
}

