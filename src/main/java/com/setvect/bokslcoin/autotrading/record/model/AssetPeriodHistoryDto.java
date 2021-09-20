package com.setvect.bokslcoin.autotrading.record.model;

import com.setvect.bokslcoin.autotrading.record.entity.AssetHistoryEntity;
import com.setvect.bokslcoin.autotrading.util.ModalMapper;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssetPeriodHistoryDto {
    private int assetHistorySeq;

    /**
     * 투 합계
     */
    private double price;

    /**
     * 평가금액
     */
    private double evlPrice;

    /**
     * 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    private double yield;
    /**
     * 투자 코인 갯수
     */
    private long coinCount;

    /**
     * 평가 날짜
     */
    private LocalDateTime regDate;


    public static AssetPeriodHistoryDto of(AssetHistoryEntity assetHistoryEntity) {
        return ModalMapper.getMapper().map(assetHistoryEntity, AssetPeriodHistoryDto.class);
    }
}
