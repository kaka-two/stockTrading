package com.kakas.stockTrading.pojo;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.kakas.stockTrading.enums.Direction;
import com.kakas.stockTrading.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order implements Comparable<Order> {

    // 订单ID
    @TableId(value="id", type= IdType.AUTO)
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

    // 更新订单
    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus orderStatus, long updateAt) {
        this.unfilledQuantity = unfilledQuantity;
        this.orderStatus = orderStatus;
        this.updateAt = updateAt;
    }
    public Order copyOrder() {
        Order copyOrder = new Order();
        copyOrder.setOrderId(this.orderId);
        copyOrder.setUserId(this.userId);
        copyOrder.setSequenceId(this.sequenceId);
        copyOrder.setDirection(this.direction);
        copyOrder.setPrice(this.price);
        copyOrder.setQuantity(this.quantity);
        copyOrder.setUnfilledQuantity(this.unfilledQuantity);
        copyOrder.setOrderStatus(this.orderStatus);
        copyOrder.setCreateAt(this.createAt);
        copyOrder.setUpdateAt(this.updateAt);
        return  copyOrder;
    }

    @Override
    public int compareTo(Order o) {
        int cmp = Long.compare(this.getUserId(), o.getUserId());
        return cmp;
    }
}
