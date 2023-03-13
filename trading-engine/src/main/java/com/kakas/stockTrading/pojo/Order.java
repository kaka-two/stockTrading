package com.kakas.stockTrading.pojo;


import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    // 订单ID
    private Long orderId;
    // 订单关联的用户ID
    private Long userId;
    // 定序ID
    private long sequenceId;

    // 订单方向：买或卖
    private Direction direction;
    // 订单价格
    private BigDecimal price;

    // 订单数量
    private BigDecimal quantity;
    // 尚未成交的数量；
    private BigDecimal unfilledQuantity;

    // 订单状态，包括等待成交、部分成交、完全成交、部分取消、完全取消
    private OrderStatus orderStatus;
    // 创建时间
    private long createAt;
    // 更新时间
    private long updateAt;


}
