package com.kakas.stockTrading.bean;

import com.kakas.stockTrading.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderBookBean {
    public static final String Empty = JsonUtil.writeJson(new OrderBookBean(0L, BigDecimal.ZERO, List.of(), List.of()));

    private Long sequenceId;

    private BigDecimal price;

    private List<OrderBookItemBean> buy;

    private List<OrderBookItemBean> sell;
}
