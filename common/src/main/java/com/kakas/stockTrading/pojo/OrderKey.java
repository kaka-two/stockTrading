package com.kakas.stockTrading.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderKey {
    private long sequenceId;
    private BigDecimal price;
}
