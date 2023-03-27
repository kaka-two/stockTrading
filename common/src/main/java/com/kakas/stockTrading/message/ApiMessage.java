package com.kakas.stockTrading.message;

import com.kakas.stockTrading.enums.ApiError;
import com.kakas.stockTrading.pojo.Order;
import lombok.Data;

/**
 * tradingEngine的调用结果
 */
@Data
public class ApiMessage implements Message {
    // null if not set.
    private String refId;

    private Long createdAt;

    // 失败时保存记录
    private ApiError error;

    // 成功时返回结果
    private Order result;

    public static ApiMessage createOrderFailed(String refId, long ts) {
        ApiMessage msg = new ApiMessage();
        msg.refId = refId;
        msg.createdAt = ts;
        msg.error = ApiError.CREATE_ORDER_FAILED;
        return msg;
    }

    public static ApiMessage cancelOrderFailed(String refId, long ts) {
        ApiMessage msg = new ApiMessage();
        msg.refId = refId;
        msg.createdAt = ts;
        msg.error = ApiError.CANCEL_ORDER_FAILED;
        return msg;
    }

    public static ApiMessage orderSuccess(String refId, long ts, Order order) {
        ApiMessage msg = new ApiMessage();
        msg.refId = refId;
        msg.createdAt = ts;
        msg.result = order;
        return msg;
    }


}
