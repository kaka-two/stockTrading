package com.kakas.stockTrading.pojo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MatchResult {
    Order takerOrder;
    List<MatchRecord> records;

    public MatchResult(Order takerOrder) {
        this.takerOrder = takerOrder;
        records = new ArrayList<>();
    }

    public void add(Order makerOrder, BigDecimal quantity, BigDecimal price) {
        records.add(new MatchRecord(takerOrder, makerOrder, quantity, price));
    }
}
