package com.kakas.stockTrading.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderBookBean {

    private Long sequenceId;

    private BigDecimal price;

    private List<OrderBookItemBean> buy;

    private List<OrderBookItemBean> sell;
}
