package com.setvect.bokslcoin.autotrading.model;


import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Account {
    private String currency = null;

    private String balance = null;

    private String locked = null;

    private String avgBuyPrice = null;

    private Boolean avgBuyPriceModified = null;

    private String unitCurrency = null;
}

