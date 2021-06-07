package com.setvect.bokslcoin.autotrading.model;

/**
 * 주문 접수
 */
public class OrderResult {
    public enum Side {
        /**
         * 매수
         */
        bid,
        /**
         * 매도
         */
        ask
    }

    /**
     * 주문 타입
     */
    public enum OrdType {
        /**
         * 지정가 주문
         */
        limit,
        /**
         * 시장가 주문(매수)
         */
        price,
        /**
         * 시장가 주문(매도)
         */
        market
    }

    /**
     * 주문 상태
     */
    public enum State {
        /**
         * 체결 대기 (default)
         */
        wait,
        /**
         * 예약주문 대기
         */
        watch,
        /**
         * 전체 체결 완료
         */
        done,
        /**
         * 주문 취소
         */
        cancel,
    }

    /**
     * 주문의 고유 아이디
     */
    private String uuid;
    /**
     * 주문 종류
     */
    private Side side;
    /**
     * 주문 방식
     */
    private OrdType ordType;
    /**
     * 주문 당시 화폐 가격
     */
    private String price;
    /**
     * 체결 가격의 평균가
     */
    private String avgPrice;
    /**
     * 주문 상태
     */
    private String state;
    /**
     * 마켓의 유일키
     */
    private String market;
    /**
     * 주문 생성 시간
     */
    private String createdAt;
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
