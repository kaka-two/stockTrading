package com.kakas.stockTrading.pojo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Assert {
    private BigDecimal available;
    private BigDecimal frozen;

    public Assert() {
        this(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public Assert(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    public BigDecimal getTotal() {
        return available.add(frozen);
    }
}
