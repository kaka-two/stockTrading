package com.kakas.stockTrading.pojo.bars;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MinBar {
    private long startTime;

    private BigDecimal openPrice;

    private BigDecimal highPrice;

    private BigDecimal lowPrice;

    private BigDecimal closePrice;

    private BigDecimal quantity;

    public static MinBar createBar(BigDecimal[] data) {
        MinBar t = new MinBar();
        t.startTime = data[0].longValue();
        t.openPrice = data[1];
        t.highPrice = data[2];
        t.lowPrice = data[3];
        t.closePrice = data[4];
        t.quantity = data[5];
        return t;
    }
}