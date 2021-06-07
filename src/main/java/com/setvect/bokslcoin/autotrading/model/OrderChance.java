package com.setvect.bokslcoin.autotrading.model;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
/**
 * 주문 가능 정보
 */
public class OrderChance {
    /**
     * 매수 수수료 비율
     */
    private String bidFee;
    /**
     * 매도 수수료 비율
     */
    private String askFee;
    /**
     * 마켓에 대한 정보
     */
    private Market market;
    /**
     * 매수 시 사용하는 화폐의 계좌 상태
     */
    private BidAccount bidAccount;
    /**
     * 매도 시 사용하는 화폐의 계좌 상태
     */
    private AskAccount askAccount;

    /**
     * 매도 시 사용하는 화폐의 계좌 상태
     */
    @Getter
    @ToString
    public static class AskAccount {
        /**
         * 화폐를 의미하는 영문 대문자 코드
         */
        private String currency;
        /**
         * 주문가능 금액/수량
         */
        private String balance;
        /**
         * 주문 중 묶여있는 금액/수량
         */
        private String locked;
        /**
         * 매수평균가
         */
        private String avgBuyPrice;
        /**
         * 매수평균가 수정 여부
         */
        private Boolean avgBuyPriceModified;
        /**
         * 평단가 기준 화폐
         */
        private String unitCurrency;
    }

    /**
     * 매수 시 사용하는 화폐의 계좌 상태
     */
    @Getter
    @ToString
    public static class BidAccount {
        /**
         * 화폐를 의미하는 영문 대문자 코드
         */
        private String currency;
        /**
         * 주문가능 금액/수량
         */
        private String balance;
        /**
         * 주문 중 묶여있는 금액/수량
         */
        private String locked;
        /**
         * 매수평균가
         */
        private String avgBuyPrice;
        /**
         * 매수평균가 수정 여부
         */
        private Boolean avgBuyPriceModified;
        /**
         * 평단가 기준 화폐
         */
        private String unitCurrency;
    }

    /**
     *
     */
    @Getter
    @ToString
    public static class Market {
        /**
         * 마켓의 유일 키
         */
        private String id;
        /**
         * 마켓 이름
         */
        private String name;
        /**
         * 지원 주문 방식
         */
        private List<OrderResult.OrdType> orderTypes;
        /**
         * 지원 주문 종류
         */
        private List<OrderResult.Side> orderSides;
        /**
         * 매수 시 제약사항
         */
        private Bid bid;
        /**
         * 매도 시 제약사항
         */
        private Ask ask;
        /**
         * 최대 매도/매수 금액
         */
        private String maxTotal;
        /**
         * 마켓 운영 상태
         */
        private String state;
    }

    /**
     * 매도 시 제약사항
     */
    @Getter
    @ToString
    public static class Ask {
        /**
         * 화폐를 의미하는 영문 대문자 코드
         */
        private String currency;
        /**
         * 주문금액 단위
         */
        private String priceUnit;
        /**
         * 최소 매도/매수 금액
         */
        private Double minTotal;
    }

    /**
     * 매수 시 제약사항
     */
    @Getter
    @ToString
    public static class Bid {
        /**
         * 화폐를 의미하는 영문 대문자 코드
         */
        private String currency;
        /**
         * 주문금액 단위
         */
        private String priceUnit;
        /**
         * 최소 매도/매수 금액
         */
        private Double minTotal;
    }
}

