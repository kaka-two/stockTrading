package com.kakas.stockTrading.message.event;

import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.message.Message;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交易请求事件，发送给交易引擎
 */
@Data
public class OrderRequestEvent  extends Event {

    private Long userId;

    private Direction direction;

    private BigDecimal price;

    private BigDecimal quantity;

}
