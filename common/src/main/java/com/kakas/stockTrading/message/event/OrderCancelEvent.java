package com.kakas.stockTrading.message.event;

import com.kakas.stockTrading.message.Message;
import lombok.Data;

/**
 * 取消订单事件，发送给交易引擎
 */
@Data
public class OrderCancelEvent extends Event {


    private Long userId;

    private Long refOrderId;
}
