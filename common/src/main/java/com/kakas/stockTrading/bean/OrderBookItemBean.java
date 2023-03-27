package com.kakas.stockTrading.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderBookItemBean {
    private BigDecimal price;
    private BigDecimal quantity;

    public void add(BigDecimal quantity) {
        this.quantity = this.quantity.add(quantity);
    }

    public OrderBookItemBean () {
        this.price = BigDecimal.ZERO;
        this.quantity = BigDecimal.ZERO;
    }
}
