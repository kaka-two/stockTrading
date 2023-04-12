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
}