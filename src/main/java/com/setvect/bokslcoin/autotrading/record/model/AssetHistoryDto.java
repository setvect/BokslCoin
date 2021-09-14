package com.setvect.bokslcoin.autotrading.record.model;

import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.util.ModalMapper;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssetHistoryDto {
    private int assetHistorySeq;

    /**
     * 자산 종류
     * KRW, BTC, ETH, ...
     */
    private String currency;

    /**
     * 원화 환산 금액
     */
    private double price;

    /**
     * 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    private double yield;

    /**
     * 자산 조회 시간
     */
    private LocalDateTime regDate;


    public static AssetHistoryDto of(AssetHistoryEntity assetHistoryEntity) {
        return ModalMapper.getMapper().map(assetHistoryEntity, AssetHistoryDto.class);
    }
}
