package com.setvect.bokslcoin.autotrading.backtest.mabs.analysis;

import com.setvect.bokslcoin.autotrading.backtest.entity.MabsConditionEntity;
import com.setvect.bokslcoin.autotrading.backtest.repository.CandleRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsConditionEntityRepository;
import com.setvect.bokslcoin.autotrading.backtest.repository.MabsTradeEntityRepository;
import com.setvect.bokslcoin.autotrading.record.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.transaction.Transactional;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
@Slf4j
public class MabsBackTest {

    @Autowired
    private MabsTradeEntityRepository mabsTradeEntityRepository;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private MabsConditionEntityRepository mabsConditionEntityRepository;

    @Test
    @Transactional
    public void backtest() {
        List<MabsConditionEntity> list = mabsConditionEntityRepository.findAll();
        list.forEach(p -> {
            System.out.printf("%s\n", p);
            System.out.printf("\t%,d\n", p.getMabsTradeEntityList().size());
        });
        System.out.println("끝");
    }
}
