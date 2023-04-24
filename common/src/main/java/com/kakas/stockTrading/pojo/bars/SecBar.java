package com.kakas.stockTrading.pojo.bars;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SecBar {
    private long startTime;

    private BigDecimal openPrice;

    private BigDecimal highPrice;

    private BigDecimal lowPrice;

    private BigDecimal closePrice;

    private BigDecimal quantity;

    public static SecBar createBar(BigDecimal[] data) {
        SecBar t = new SecBar();
        t.startTime = data[0].longValue();
        t.openPrice = data[1];
        t.highPrice = data[2];
        t.lowPrice = data[3];
        t.closePrice = data[4];
        t.quantity = data[5];
        return t;
    }
}
