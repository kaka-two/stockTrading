package com.kakas.stockTrading.bean;

import com.kakas.stockTrading.enums.MatchType;

import java.math.BigDecimal;

public record SimpleMatchDetailBean(BigDecimal price, BigDecimal quantity, MatchType type) {
}
