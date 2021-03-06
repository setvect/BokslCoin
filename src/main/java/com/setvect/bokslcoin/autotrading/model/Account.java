package com.setvect.bokslcoin.autotrading.model;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Optional;

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

    public double getAvgBuyPriceValue() {
        if (avgBuyPrice == null) {
            return 0;
        }
        return Double.parseDouble(avgBuyPrice);
    }

    public double getBalanceValue() {
        if (balance == null) {
            return 0;
        }
        return Double.parseDouble(balance);
    }

    public double getBalanceValueWithLock() {
        if (balance == null) {
            return 0;
        }
        return Double.parseDouble(balance) + Optional.ofNullable(locked).map(Double::parseDouble).orElse(0.0);
    }

    /**
     * @return 구입금액
     */
    public double getInvestCash() {
        return getAvgBuyPriceValue() * getBalanceValue();
    }

    /**
     * @return 구입금액(주문 묶어있는 거 포함)
     */
    public double getInvestCashWithLock() {
        return getAvgBuyPriceValue() * getBalanceValueWithLock();
    }
}

