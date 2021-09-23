package com.setvect.bokslcoin.autotrading.record.model;

import com.setvect.bokslcoin.autotrading.record.entity.TradeEntity;
import com.setvect.bokslcoin.autotrading.util.ModalMapper;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TradeDto {
    private int tradeSeq;

    /**
     * 코인 이름
     * KRW-BTC, KRW-ETH, ...
     */
    private String market;

    /**
     * 매수/매도
     */
    private TradeEntity.TradeType tradeType;

    /**
     * 거래 금액
     */
    private double amount;

    /**
     * 거래 단가
     */
    private double unitPrice;

    /**
     * 매도 수익률
     */
    private double yield;

    /**
     * 거래 시간
     */
    private LocalDateTime regDate;

    public static TradeDto of(TradeEntity tradeEntity) {
        return ModalMapper.getMapper().map(tradeEntity, TradeDto.class);
    }
}
