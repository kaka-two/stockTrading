package com.kakas.stockTrading.pojo;

import java.math.BigDecimal;

public record MatchRecord(Order takerOrder, Order makerOrder, BigDecimal quantity, BigDecimal price) {
}
