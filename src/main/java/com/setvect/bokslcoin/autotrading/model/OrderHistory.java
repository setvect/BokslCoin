package com.setvect.bokslcoin.autotrading.model;

import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 주문 내역
 */
@Getter
@ToString
public class OrderHistory {
    /**
     * 주문의 고유 아이디
     */
    private String uuid;
    /**
     * 주문 종류
     */
    private OrderResult.Side side;
    /**
     * 주문 방식
     */
    private OrderResult.OrdType ordType;
    /**
     * 주문 당시 화폐 가격
     */
    private String price;
    /**
     * 주문 상태
     */
    private OrderResult.State state;
    /**
     * 마켓의 유일키
     */
    private String market;
    /**
     * 주문 생성 시간
     */
    private LocalDateTime createdAt;
    /**
     * 사용자가 입력한 주문 양
     */
    private String volume;
    /**
     * 체결 후 남은 주문 양
     */
    private String remainingVolume;
    /**
     * 수수료로 예약된 비용
     */
    private String reservedFee;
    /**
     * 남은 수수료
     */
    private String remainingFee;
    /**
     * 사용된 수수료
     */
    private String paidFee;
    /**
     * 거래에 사용중인 비용
     */
    private String locked;
    /**
     * 체결된 양
     */
    private String executedVolume;
    /**
     * 해당 주문에 걸린 체결 수
     */
    private Integer tradeCount;
}
