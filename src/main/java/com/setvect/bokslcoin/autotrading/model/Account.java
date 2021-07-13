package com.setvect.bokslcoin.autotrading.model;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Account {
    /**
     * 화폐를 의미하는 영문 대문자 코드
     */
    private String currency;
    /**
     * 주문가능 금액/수량
     */
    private String balance = "0";
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

    public String getMarket() {
        if (getCurrency().equals("KRW")) {
            return getCurrency();
        }
        return getUnitCurrency() + "-" + getCurrency();
    }

    public double getInvestCash() {
        return Double.valueOf(getAvgBuyPrice()) * Double.valueOf(getBalance());
    }
}

